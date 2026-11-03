package com.example.data

import com.example.domain.DEFAULT_DEMO_PROTOCOLS
import com.example.domain.DemoProtocolConfig
import com.example.domain.PhaseEngineSnapshot
import com.example.domain.PlanGroup
import com.example.domain.RunStatus
import com.example.domain.TimeProvider

/**
 * Репозиторий хранения планов, дневных снимков (DailyPlan + DailySlot), попыток (WorkoutRun),
 * контрольных точек и атомарных завершений (WorkoutCompletion) в Room.
 *
 * Реализует правила разделов 6, 7 и 9 ТЗ v1.0:
 * - Переменное число подходов в плане (включая N=0 — «Сегодня без тренировок», без зашитой 9).
 * - Демо-записи отделены признаком `isDemo = true` от реальных записей (`isDemo = false`).
 * - Снимок дня создаётся один раз для `(localDate, isDemo)` и повторно используется при возврате к этой дате.
 * - Попытка, начатая до полуночи, хранит `startDate` момента старта и завершается в свой `slotId` без дублей.
 * - Завершённый слот нельзя запустить повторно в ту же дату; остановленная без зачёта попытка (`STOPPED`) не блокирует слот.
 * - При перезапуске приложения невыполненные попытки из погибшего процесса (`PREPARING` / `RUNNING`)
 *   переводятся в `INTERRUPTED` на последней подтверждённой контрольной точке без начисления времени простоя.
 */
class WorkoutPersistenceRepository(
    private val dao: VelarisDao,
    private val timeProvider: TimeProvider
) {

    suspend fun ensureBuiltinDemoPlansSeeded() {
        val existingStandard = dao.getTrainingPlanById(DEMO_PLAN_STANDARD_3)
        if (existingStandard == null) {
            dao.upsertTrainingPlan(
                TrainingPlanEntity(
                    id = DEMO_PLAN_STANDARD_3,
                    version = 1,
                    titleRu = "Демо-план: 3 подхода (Утро / Обед / Вечер)",
                    source = "DEMO_BUILTIN_TESTBED",
                    effectiveFromDate = "2026-01-01",
                    isDemo = true
                )
            )
            val assignments = DEFAULT_DEMO_PROTOCOLS.mapIndexed { idx, proto ->
                PlanAssignmentEntity(
                    id = proto.id,
                    planId = DEMO_PLAN_STANDARD_3,
                    planVersion = 1,
                    groupCode = proto.group.code,
                    sortOrder = idx + 1,
                    titleRu = proto.titleRu,
                    contractionMs = proto.contractionMs,
                    relaxationMs = proto.relaxationMs,
                    repetitions = proto.repetitions,
                    isDemo = true
                )
            }
            dao.upsertPlanAssignments(assignments)
        }

        val existingRestDay = dao.getTrainingPlanById(DEMO_PLAN_ZERO_SLOTS)
        if (existingRestDay == null) {
            dao.upsertTrainingPlan(
                TrainingPlanEntity(
                    id = DEMO_PLAN_ZERO_SLOTS,
                    version = 1,
                    titleRu = "Демо-план: Сегодня без тренировок (N=0)",
                    source = "DEMO_BUILTIN_REST_DAY",
                    effectiveFromDate = "2026-01-01",
                    isDemo = true
                )
            )
        }
    }

    /**
     * Получает или создаёт снимок дневного плана (`DailyPlan` + `DailySlot`s) для указанной локальной даты.
     * Существующий снимок дня и завершённые слоты никогда не пересоздаются и не переименовываются.
     */
    suspend fun getOrCreateDailySlots(
        localDate: String = timeProvider.currentLocalDate(),
        planId: String = DEMO_PLAN_STANDARD_3,
        isDemo: Boolean = true
    ): List<DailySlotWithCompletion> {
        ensureBuiltinDemoPlansSeeded()

        var dailyPlan = dao.getDailyPlan(localDate = localDate, isDemo = isDemo)
        if (dailyPlan == null) {
            val plan = dao.getTrainingPlanById(planId) ?: TrainingPlanEntity(
                id = planId,
                version = 1,
                titleRu = "Демо-план",
                source = "DEMO_BUILTIN",
                effectiveFromDate = localDate,
                isDemo = isDemo
            )
            val dailyPlanId = "${if (isDemo) "demo" else "real"}_${localDate}"
            val newDailyPlan = DailyPlanEntity(
                id = dailyPlanId,
                localDate = localDate,
                planId = plan.id,
                planVersion = plan.version,
                isDemo = isDemo
            )
            dao.insertDailyPlanIgnore(newDailyPlan)
            dailyPlan = dao.getDailyPlan(localDate = localDate, isDemo = isDemo) ?: newDailyPlan

            val assignments = dao.getAssignmentsForPlan(plan.id, plan.version)
            if (assignments.isNotEmpty()) {
                val maxOrderByGroup = assignments.groupBy { it.groupCode }
                    .mapValues { (_, list) -> list.maxOf { it.sortOrder } }

                val slots = assignments.map { a ->
                    DailySlotEntity(
                        id = "${dailyPlan.id}_${a.id}",
                        dailyPlanId = dailyPlan.id,
                        localDate = localDate,
                        assignmentId = a.id,
                        groupCode = a.groupCode,
                        sortOrder = a.sortOrder,
                        titleRu = a.titleRu,
                        contractionMs = a.contractionMs,
                        relaxationMs = a.relaxationMs,
                        repetitions = a.repetitions,
                        isLastInGroup = (maxOrderByGroup[a.groupCode] == a.sortOrder),
                        isDemo = isDemo
                    )
                }
                dao.insertDailySlotsIgnore(slots)
            }
        }

        val slots = dao.getSlotsForDailyPlan(dailyPlan.id)
        val completionsBySlot = dao.getCompletionsForDate(localDate = localDate, isDemo = isDemo)
            .associateBy { it.slotId }

        return slots.map { slot ->
            DailySlotWithCompletion(
                slot = slot,
                completion = completionsBySlot[slot.id]
            )
        }
    }

    /**
     * Проверяет, завершён ли уже данный слот. Завершённый слот нельзя выполнить повторно в ту же дату (ТЗ п. 5, п. 9).
     */
    suspend fun isSlotCompleted(slotId: String): Boolean {
        return dao.getCompletionForSlot(slotId) != null
    }

    /**
     * Сохраняет контрольную точку (при старте, смене фаз, паузе, возобновлении, остановке и каждые ~5 сек).
     */
    suspend fun saveRunCheckpoint(snapshot: PhaseEngineSnapshot) {
        val runId = snapshot.runId ?: return
        val slotId = snapshot.slotId ?: return
        val existing = dao.getRunById(runId)
        val nowWall = timeProvider.currentWallTimeMs()

        val entity = WorkoutRunEntity(
            id = runId,
            slotId = slotId,
            status = snapshot.status.name,
            startedAtEpochMs = existing?.startedAtEpochMs ?: nowWall,
            startDate = snapshot.startDate,
            zoneId = snapshot.zoneId,
            planVersion = existing?.planVersion ?: 1,
            titleRu = snapshot.protocol.titleRu,
            groupCode = snapshot.protocol.group.code,
            contractionMs = snapshot.protocol.contractionMs,
            relaxationMs = snapshot.protocol.relaxationMs,
            repetitions = snapshot.protocol.repetitions,
            isLastInGroup = snapshot.protocol.isLastInGroup,
            soundPackIdSnapshot = snapshot.soundPackSnapshot.id,
            metronomeStyleIdSnapshot = snapshot.metronomeStyleSnapshot.id,
            confirmedActiveMs = snapshot.confirmedActiveMs,
            currentRepetition = snapshot.currentRepetition,
            currentPhase = snapshot.currentPhase.name,
            phaseRemainingMs = snapshot.phaseRemainingMs,
            checkpointWallEpochMs = nowWall,
            bootSessionId = timeProvider.bootSessionId(),
            interruptionReason = snapshot.interruptionReason,
            isDemo = snapshot.isDemo
        )
        if (existing == null) {
            dao.insertRun(entity)
        } else {
            dao.updateRun(entity)
        }
    }

    /**
     * Завершает попытку и записывает результат одной атомарной транзакцией Room.
     * Возвращает `true`, если создано новое завершение, и `false`, если этот `slotId` уже был зачтён.
     */
    suspend fun completeRunAtomically(
        snapshot: PhaseEngineSnapshot,
        optionalDifficulty: String? = null
    ): Boolean {
        saveRunCheckpoint(snapshot)
        val runId = snapshot.runId ?: return false
        return dao.completeRunAtomically(
            runId = runId,
            completedAtEpochMs = timeProvider.currentWallTimeMs(),
            optionalDifficulty = optionalDifficulty
        )
    }

    /**
     * При запуске приложения/сервиса после выгрузки процесса или перезагрузки телефона:
     * переводит любые незавершённые попытки (`PREPARING` / `RUNNING` / `PAUSED`) в `INTERRUPTED`,
     * сохраняя только подтверждённое `confirmedActiveMs` без добавления времени простоя.
     */
    suspend fun recoverInterruptedRunAfterProcessDeath(): WorkoutRunEntity? {
        val candidates = dao.getActiveOrInterruptedRuns()
        if (candidates.isEmpty()) return null

        val primary = candidates.first()
        // Остальные повисшие записи (если вдруг были) закрываем как STOPPED
        for (extra in candidates.drop(1)) {
            dao.updateRun(
                extra.copy(
                    status = RunStatus.STOPPED.name,
                    interruptionReason = "Закрыто при проверке целостности единственной активной попытки"
                )
            )
        }

        // Если слот уже завершён, попытку восстанавливать не нужно
        if (dao.getCompletionForSlot(primary.slotId) != null) {
            dao.updateRun(primary.copy(status = RunStatus.COMPLETED.name))
            return null
        }

        val recovered = if (primary.status == RunStatus.PREPARING.name || primary.status == RunStatus.RUNNING.name) {
            val reason = if (primary.bootSessionId != timeProvider.bootSessionId()) {
                "Устройство перезагружалось — попытка сохранена на последней подтверждённой точке без автозапуска"
            } else {
                "Процесс приложения был выгружен системой — восстановлена последняя контрольная точка без времени простоя"
            }
            val updated = primary.copy(
                status = RunStatus.INTERRUPTED.name,
                interruptionReason = reason,
                checkpointWallEpochMs = timeProvider.currentWallTimeMs()
            )
            dao.updateRun(updated)
            updated
        } else {
            primary
        }

        return recovered
    }

    suspend fun countRealCompletions(): Int = dao.countCompletionsByDemoFlag(isDemo = false)
    suspend fun countDemoCompletions(): Int = dao.countCompletionsByDemoFlag(isDemo = true)

    fun slotToProtocolConfig(slot: DailySlotEntity): DemoProtocolConfig {
        return DemoProtocolConfig(
            id = slot.id,
            titleRu = slot.titleRu,
            contractionMs = slot.contractionMs,
            relaxationMs = slot.relaxationMs,
            repetitions = slot.repetitions,
            isLastInGroup = slot.isLastInGroup,
            group = PlanGroup.fromCode(slot.groupCode)
        )
    }

    companion object {
        const val DEMO_PLAN_STANDARD_3 = "demo_plan_standard_3"
        const val DEMO_PLAN_ZERO_SLOTS = "demo_plan_zero_slots"
    }
}
