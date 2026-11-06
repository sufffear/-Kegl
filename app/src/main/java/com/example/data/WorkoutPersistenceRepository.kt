package com.example.data

import com.example.domain.CustomWorkoutProgram
import com.example.domain.DEFAULT_DEMO_PROTOCOLS
import com.example.domain.DemoProtocolConfig
import com.example.domain.PhaseEngineSnapshot
import com.example.domain.PlanGroup
import com.example.domain.RunStatus
import com.example.domain.TimeProvider
import com.example.domain.WorkoutDifficulty
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class WeeklyDaySummary(
    val localDate: String,
    val shortLabelRu: String,
    val dayOfMonthLabel: String,
    val isToday: Boolean,
    val completedCount: Int,
    val assignedCount: Int,
    val plannedCompletedCount: Int,
    val freeCompletedCount: Int,
    val completedTitlesRu: List<String>
)

data class WeeklyOverviewState(
    val days: List<WeeklyDaySummary> = emptyList(),
    val selectedDate: String = "",
    val totalRealCompletionsInWeek: Int = 0,
    val totalRealCompletionsAllTime: Int = 0
) {
    val hasAnyRealData: Boolean
        get() = totalRealCompletionsAllTime > 0 || totalRealCompletionsInWeek > 0

    val selectedDaySummary: WeeklyDaySummary?
        get() = days.firstOrNull { it.localDate == selectedDate } ?: days.lastOrNull()
}

/**
 * Репозиторий хранения планов, дневных снимков (DailyPlan + DailySlot), попыток (WorkoutRun),
 * контрольных точек, атомарных завершений (WorkoutCompletion) и 7-дневного обзора в Room.
 */
class WorkoutPersistenceRepository(
    private val dao: VelarisDao,
    private val timeProvider: TimeProvider
) {

    suspend fun ensureBuiltinPlansSeeded() {
        val existingStandardDemo = dao.getTrainingPlanById(DEMO_PLAN_STANDARD_3)
        if (existingStandardDemo == null) {
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

        val existingRealPlan = dao.getTrainingPlanById(REAL_PLAN_STANDARD_3)
        if (existingRealPlan == null || existingRealPlan.version < REAL_PLAN_CURRENT_VERSION) {
            dao.upsertTrainingPlan(
                TrainingPlanEntity(
                    id = REAL_PLAN_STANDARD_3,
                    version = REAL_PLAN_CURRENT_VERSION,
                    titleRu = "Ежедневный план: Утро, Обед и Вечер (по 3 тренировки)",
                    source = BenchSignalSettings.DEFAULT_REAL_PROGRAM_SOURCE,
                    effectiveFromDate = "2026-01-01",
                    isDemo = false
                )
            )
            val groups = listOf(PlanGroup.MORNING, PlanGroup.NOON, PlanGroup.EVENING)
            val realAssignments = mutableListOf<PlanAssignmentEntity>()
            var orderCounter = 1
            for (group in groups) {
                val gKey = group.code.lowercase()
                // 1. Быстрая тренировка: 100 сжиманий (1 с сжатие / 1 с отдых)
                realAssignments.add(
                    PlanAssignmentEntity(
                        id = "real_v2_${gKey}_fast",
                        planId = REAL_PLAN_STANDARD_3,
                        planVersion = REAL_PLAN_CURRENT_VERSION,
                        groupCode = group.code,
                        sortOrder = orderCounter++,
                        titleRu = "Быстрая тренировка",
                        contractionMs = 1_000L,
                        relaxationMs = 1_000L,
                        repetitions = 100,
                        isDemo = false
                    )
                )
                // 2. Медленная тренировка (Силовой контроль): 10 сжиманий (10 с сжатие / 5 с отдых)
                realAssignments.add(
                    PlanAssignmentEntity(
                        id = "real_v2_${gKey}_slow",
                        planId = REAL_PLAN_STANDARD_3,
                        planVersion = REAL_PLAN_CURRENT_VERSION,
                        groupCode = group.code,
                        sortOrder = orderCounter++,
                        titleRu = "Силовой контроль",
                        contractionMs = 10_000L,
                        relaxationMs = 5_000L,
                        repetitions = 10,
                        isDemo = false
                    )
                )
                // 3. Очень медленные сжимания (Выносливость): 10 сжиманий (25 с удержание / 10 с отдых)
                realAssignments.add(
                    PlanAssignmentEntity(
                        id = "real_v2_${gKey}_endurance",
                        planId = REAL_PLAN_STANDARD_3,
                        planVersion = REAL_PLAN_CURRENT_VERSION,
                        groupCode = group.code,
                        sortOrder = orderCounter++,
                        titleRu = "Выносливость",
                        contractionMs = 25_000L,
                        relaxationMs = 10_000L,
                        repetitions = 10,
                        isDemo = false
                    )
                )
            }
            dao.upsertPlanAssignments(realAssignments)
        }
    }

    suspend fun ensureBuiltinDemoPlansSeeded() {
        ensureBuiltinPlansSeeded()
    }

    suspend fun getOrCreateDailySlots(
        localDate: String = timeProvider.currentLocalDate(),
        planId: String = DEMO_PLAN_STANDARD_3,
        isDemo: Boolean = true
    ): List<DailySlotWithCompletion> {
        ensureBuiltinPlansSeeded()

        val plan = dao.getTrainingPlanById(planId) ?: TrainingPlanEntity(
            id = planId,
            version = if (planId == REAL_PLAN_STANDARD_3) REAL_PLAN_CURRENT_VERSION else 1,
            titleRu = if (isDemo) "Демо-план" else "Ежедневный план",
            source = if (isDemo) "DEMO_BUILTIN" else BenchSignalSettings.DEFAULT_REAL_PROGRAM_SOURCE,
            effectiveFromDate = localDate,
            isDemo = isDemo
        )
        val assignments = dao.getAssignmentsForPlan(plan.id, plan.version)

        var dailyPlan = dao.getDailyPlan(localDate = localDate, isDemo = isDemo)
        val needsSlotSync = dailyPlan == null ||
            dailyPlan.planId != plan.id ||
            dailyPlan.planVersion != plan.version

        if (dailyPlan == null) {
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
        } else if (needsSlotSync) {
            val updatedDailyPlan = dailyPlan.copy(
                planId = plan.id,
                planVersion = plan.version
            )
            dao.insertDailyPlans(listOf(updatedDailyPlan))
            dailyPlan = updatedDailyPlan
        }

        if (needsSlotSync && assignments.isNotEmpty()) {
            val maxOrderByGroup = assignments.groupBy { it.groupCode }
                .mapValues { (_, list) -> list.maxOf { it.sortOrder } }

            val slotsToInsert = assignments.map { a ->
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
            dao.insertDailySlotsIgnore(slotsToInsert)
        }

        val rawSlots = dao.getSlotsForDailyPlan(dailyPlan.id)
        val validAssignmentIds = assignments.map { it.id }.toSet()
        val slots = if (plan.id == DEMO_PLAN_ZERO_SLOTS) {
            emptyList()
        } else if (validAssignmentIds.isNotEmpty()) {
            rawSlots.filter { it.assignmentId in validAssignmentIds }
        } else {
            rawSlots
        }

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
     * Формирует компактный 7-дневный обзор строго по реальным сохранённым записям (`isDemo = false`).
     * Демо-результаты (`isDemo = true`) не попадают в реальную статистику.
     */
    suspend fun loadWeeklyOverview(
        todayDateStr: String = timeProvider.currentLocalDate(),
        selectedDateStr: String? = null
    ): WeeklyOverviewState {
        val baseDate = runCatching { LocalDate.parse(todayDateStr) }.getOrElse { LocalDate.now() }
        val dates = (6 downTo 0).map { offset -> baseDate.minusDays(offset.toLong()) }
        val dateKeys = dates.map { it.format(DateTimeFormatter.ISO_LOCAL_DATE) }

        val realSlotsByDate = dao.getSlotsForDates(localDates = dateKeys, isDemo = false)
            .groupBy { it.localDate }
        val realCompletionsByDate = dao.getCompletionsForDates(localDates = dateKeys, isDemo = false)
            .groupBy { it.localDate }
        val allSlotsMap = realSlotsByDate.values.flatten().associateBy { it.id }

        val days = dates.map { date ->
            val key = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val daySlots = realSlotsByDate[key].orEmpty()
            val dayCompletions = realCompletionsByDate[key].orEmpty()
            val plannedCompletions = dayCompletions.filter { !it.isFreeMode }
            val freeCompletions = dayCompletions.filter { it.isFreeMode }
            val completedTitles = dayCompletions.map { comp ->
                if (comp.isFreeMode) {
                    "Свободный подход"
                } else {
                    val slotEntity = allSlotsMap[comp.slotId]
                    if (slotEntity != null) {
                        val groupTitle = PlanGroup.fromCode(slotEntity.groupCode).titleRu
                        if (slotEntity.titleRu.startsWith(groupTitle)) {
                            slotEntity.titleRu
                        } else {
                            "$groupTitle · ${slotEntity.titleRu}"
                        }
                    } else {
                        "Подход выполнен"
                    }
                }
            }
            WeeklyDaySummary(
                localDate = key,
                shortLabelRu = dayOfWeekShortRu(date.dayOfWeek.value),
                dayOfMonthLabel = date.dayOfMonth.toString(),
                isToday = (key == todayDateStr),
                completedCount = dayCompletions.size,
                assignedCount = daySlots.size,
                plannedCompletedCount = plannedCompletions.size,
                freeCompletedCount = freeCompletions.size,
                completedTitlesRu = completedTitles
            )
        }

        val totalWeek = days.sumOf { it.completedCount }
        val totalAllTime = dao.countCompletionsByDemoFlag(isDemo = false)
        val resolvedSelected = if (selectedDateStr != null && dateKeys.contains(selectedDateStr)) {
            selectedDateStr
        } else {
            todayDateStr
        }

        return WeeklyOverviewState(
            days = days,
            selectedDate = resolvedSelected,
            totalRealCompletionsInWeek = totalWeek,
            totalRealCompletionsAllTime = totalAllTime
        )
    }

    private fun dayOfWeekShortRu(isoDayOfWeek: Int): String = when (isoDayOfWeek) {
        1 -> "Пн"
        2 -> "Вт"
        3 -> "Ср"
        4 -> "Чт"
        5 -> "Пт"
        6 -> "Сб"
        7 -> "Вс"
        else -> "Дн"
    }

    suspend fun updateCompletionDifficulty(slotId: String, difficulty: WorkoutDifficulty?) {
        dao.updateCompletionDifficulty(slotId = slotId, difficulty = difficulty?.code)
    }

    suspend fun isSlotCompleted(slotId: String): Boolean {
        return dao.getCompletionForSlot(slotId) != null
    }

    suspend fun ensureFreeTemplatesSeeded() {
        val existing = dao.getAllFreeTemplates()
        if (existing.isEmpty()) {
            dao.upsertFreeTemplates(FreeIntervalTemplateEntity.createInitialDefaults())
        }
    }

    suspend fun getAllFreeTemplates(): List<FreeIntervalTemplateEntity> {
        val list = dao.getAllFreeTemplates()
        return if (list.isEmpty()) {
            val defaults = FreeIntervalTemplateEntity.createInitialDefaults()
            dao.upsertFreeTemplates(defaults)
            defaults
        } else {
            list
        }
    }

    suspend fun saveFreeTemplate(template: FreeIntervalTemplateEntity) {
        dao.upsertFreeTemplate(template.copy(updatedAtEpochMs = timeProvider.currentWallTimeMs()))
    }

    // --- Продвинутый конструктор тренировок (программы из 4 типов блоков) ---

    suspend fun getAllCustomPrograms(): List<CustomWorkoutProgram> {
        val entities = dao.getAllCustomPrograms()
        if (entities.isEmpty()) {
            val defaults = CustomProgramJsonParser.createDefaultPrograms()
            dao.upsertCustomPrograms(defaults.mapIndexed { idx, prog -> CustomProgramJsonParser.toEntity(prog, idx) })
            return defaults
        }
        return entities.map { CustomProgramJsonParser.toDomain(it) }
    }

    suspend fun saveCustomProgram(program: CustomWorkoutProgram, sortOrder: Int = 0) {
        val updated = program.copy(updatedAtEpochMs = timeProvider.currentWallTimeMs())
        dao.upsertCustomProgram(CustomProgramJsonParser.toEntity(updated, sortOrder))
    }

    suspend fun deleteCustomProgram(id: String) {
        dao.deleteCustomProgram(id)
    }

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
            isDemo = snapshot.isDemo,
            isFreeMode = snapshot.isFreeMode
        )
        if (existing == null) {
            dao.insertRun(entity)
        } else {
            dao.updateRun(entity)
        }
    }

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

    suspend fun recoverInterruptedRunAfterProcessDeath(): WorkoutRunEntity? {
        val candidates = dao.getActiveOrInterruptedRuns()
        if (candidates.isEmpty()) return null

        val primary = candidates.first()
        for (extra in candidates.drop(1)) {
            dao.updateRun(
                extra.copy(
                    status = RunStatus.STOPPED.name,
                    interruptionReason = "Закрыто при проверке целостности единственной активной попытки"
                )
            )
        }

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
        val group = PlanGroup.fromCode(slot.groupCode)
        val displayTitle = if (slot.titleRu.startsWith(group.titleRu)) {
            slot.titleRu
        } else {
            "${group.titleRu} · ${slot.titleRu}"
        }
        return DemoProtocolConfig(
            id = slot.id,
            titleRu = displayTitle,
            contractionMs = slot.contractionMs,
            relaxationMs = slot.relaxationMs,
            repetitions = slot.repetitions,
            isLastInGroup = slot.isLastInGroup,
            group = group
        )
    }

    companion object {
        const val DEMO_PLAN_STANDARD_3 = "demo_plan_standard_3"
        const val DEMO_PLAN_ZERO_SLOTS = "demo_plan_zero_slots"
        const val REAL_PLAN_STANDARD_3 = "real_plan_standard_3"
        const val REAL_PLAN_CURRENT_VERSION = 2
    }
}
