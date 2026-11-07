package com.example.domain

/**
 * Чистый Kotlin-движок фаз интервального таймера (тестируется без экрана и без Android-контекста).
 * Реализует правила разделов 6, 7, 8 и 9 ТЗ v1.0:
 * - Состояния: IDLE, PREPARING, RUNNING, PAUSED, INTERRUPTED, COMPLETED, STOPPED.
 * - Максимум одна активная попытка.
 * - Подготовка 3 секунды (3 коротких сигнала), не входит в активное время.
 * - При совпадении тика метронома и команды фазы тик пропускается.
 * - На паузе всегда отображается «Пауза — расслабьтесь».
 * - Продолжение вручную: 3 сигнала подготовки, затем остаток текущей фазы.
 * - Остановка: сначала пауза, затем диалог «Завершить без зачёта?»; отмена диалога оставляет паузу.
 * - Защита от задержки планировщика ОС: если тик задержан сильнее порога, переход в INTERRUPTED без пачки сигналов.
 * - Контрольные точки (Checkpoints): вызываются при старте, смене фаз, паузе, возобновлении, остановке,
 *   завершении и каждые ~5 секунд активной работы (НЕ на каждом кадре).
 * - Восстановление прерванной попытки (restoreInterruptedRun): восстанавливает состояние из последней
 *   подтверждённой контрольной точки без начисления времени простоя.
 */
class PhaseEngine(
    private val clock: MonotonicClock,
    private val onSignal: (SignalType, SoundPack, MetronomeTickStyle) -> Unit = { _, _, _ -> },
    private val onCheckpoint: (PhaseEngineSnapshot, CheckpointTrigger) -> Unit = { _, _ -> },
    private val onLog: (category: String, message: String) -> Unit = { _, _ -> },
    val preparationDurationMs: Long = 3_000L,
    val expectedTickIntervalMs: Long = 50L,
    val schedulerDelayThresholdMs: Long = 2_500L,
    val periodicCheckpointIntervalMs: Long = 5_000L
) {
    private var snapshot = PhaseEngineSnapshot()
    private var lastTickRealtimeMs: Long = 0L
    private var lastPrepSecondEmitted: Int = -1
    private var lastMetronomeSecondEmitted: Int = -1
    private var lastCheckpointActiveMs: Long = 0L
    private var customExecutionSteps: List<ExpandedPhaseStep> = emptyList()
    private var currentCustomStepIndex: Int = 0
    var isMetronomeEnabled: Boolean = true

    fun currentSnapshot(): PhaseEngineSnapshot = snapshot

    fun selectProtocolIfIdle(protocol: DemoProtocolConfig): Boolean {
        if (snapshot.isActiveOrPaused) {
            onLog("ENGINE", "Отклонена смена протокола во время активной попытки (${snapshot.status})")
            return false
        }
        customExecutionSteps = emptyList()
        currentCustomStepIndex = 0
        val now = clock.elapsedRealtimeMs()
        snapshot = PhaseEngineSnapshot(
            status = RunStatus.IDLE,
            protocol = protocol,
            currentRepetition = 1,
            totalRepetitions = protocol.repetitions,
            currentPhase = PhaseType.CONTRACTION,
            phaseRemainingMs = protocol.contractionMs,
            phaseDurationMs = protocol.contractionMs,
            prepRemainingMs = 0L,
            confirmedActiveMs = 0L,
            totalActiveDurationMs = protocol.totalActiveDurationMs,
            remainingActiveMs = protocol.totalActiveDurationMs,
            snapshotElapsedRealtimeMs = now
        )
        onLog("PROTOCOL", "Выбран слот/протокол: ${protocol.titleRu}")
        return true
    }

    /**
     * Запуск новой попытки.
     * Снимок протокола, даты старта, часового пояса и звукового набора фиксируется на всё время подхода.
     */
    fun start(
        protocol: DemoProtocolConfig = snapshot.protocol,
        runId: String = "run_${clock.elapsedRealtimeMs()}",
        slotId: String = protocol.id,
        startDate: String = "2026-11-03",
        zoneId: String = "UTC",
        soundPackSnapshot: SoundPack = SoundPack.SOFT,
        metronomeStyleSnapshot: MetronomeTickStyle = MetronomeTickStyle.SOFT,
        isDemo: Boolean = true,
        isFreeMode: Boolean = false
    ): Boolean {
        if (snapshot.isActiveOrPaused) {
            onLog("GUARD", "Повторный быстрый старт заблокирован: уже активно состояние ${snapshot.status}")
            return false
        }
        val now = clock.elapsedRealtimeMs()
        customExecutionSteps = emptyList()
        currentCustomStepIndex = 0
        lastTickRealtimeMs = now
        lastPrepSecondEmitted = 3
        lastMetronomeSecondEmitted = -1
        lastCheckpointActiveMs = 0L

        snapshot = PhaseEngineSnapshot(
            runId = runId,
            slotId = slotId,
            startDate = startDate,
            zoneId = zoneId,
            status = RunStatus.PREPARING,
            protocol = protocol,
            soundPackSnapshot = soundPackSnapshot,
            metronomeStyleSnapshot = metronomeStyleSnapshot,
            currentRepetition = 1,
            totalRepetitions = protocol.repetitions,
            currentPhase = PhaseType.CONTRACTION,
            phaseRemainingMs = protocol.contractionMs,
            phaseDurationMs = protocol.contractionMs,
            prepRemainingMs = preparationDurationMs,
            confirmedActiveMs = 0L,
            totalActiveDurationMs = protocol.totalActiveDurationMs,
            remainingActiveMs = protocol.totalActiveDurationMs,
            snapshotElapsedRealtimeMs = now,
            isStopDialogVisible = false,
            interruptionReason = null,
            lastSchedulerDriftMs = 0L,
            maxSchedulerDriftMs = 0L,
            isDemo = isDemo,
            isFreeMode = isFreeMode
        )
        onLog(
            "STATE",
            "IDLE -> PREPARING (слот=$slotId, дата=$startDate, звук=${soundPackSnapshot.titleRu}, сигнал 1/3)"
        )
        onCheckpoint(snapshot, CheckpointTrigger.START)
        emitSignal(SignalType.PREP_TICK)
        return true
    }

    /**
     * Запуск программы продвинутого конструктора тренировок (составные блоки).
     */
    fun startCustomProgram(
        program: CustomWorkoutProgram,
        runId: String = "prog_run_${clock.elapsedRealtimeMs()}",
        startDate: String = "2026-11-03",
        zoneId: String = "UTC",
        soundPackSnapshot: SoundPack = SoundPack.SOFT,
        metronomeStyleSnapshot: MetronomeTickStyle = MetronomeTickStyle.SOFT,
        isDemo: Boolean = false
    ): Boolean {
        if (snapshot.isActiveOrPaused) {
            onLog("GUARD", "Повторный старт заблокирован: активно состояние ${snapshot.status}")
            return false
        }
        val steps = program.expandAllSteps()
        if (steps.isEmpty()) {
            onLog("GUARD", "Нельзя запустить пустую программу без блоков")
            return false
        }

        customExecutionSteps = steps
        currentCustomStepIndex = 0

        val firstStep = steps[0]
        val totalMs = program.totalDurationMs
        val now = clock.elapsedRealtimeMs()
        lastTickRealtimeMs = now
        lastPrepSecondEmitted = 3
        lastMetronomeSecondEmitted = -1
        lastCheckpointActiveMs = 0L

        // Синтезируем виртуальный протокол для совместимости с внешними слушателями
        val proto = DemoProtocolConfig(
            id = program.id,
            titleRu = program.title,
            contractionMs = firstStep.durationMs.coerceIn(1_000L, 120_000L),
            relaxationMs = 5_000L,
            repetitions = program.totalCycles.coerceIn(1, 300),
            isLastInGroup = true,
            group = PlanGroup.MORNING
        )

        snapshot = PhaseEngineSnapshot(
            runId = runId,
            slotId = "prog_${program.id}_$runId",
            startDate = startDate,
            zoneId = zoneId,
            status = RunStatus.PREPARING,
            protocol = proto,
            soundPackSnapshot = soundPackSnapshot,
            metronomeStyleSnapshot = metronomeStyleSnapshot,
            currentRepetition = firstStep.cycleIndex,
            totalRepetitions = firstStep.totalCyclesInBlock,
            currentPhase = firstStep.phaseType,
            phaseRemainingMs = firstStep.durationMs,
            phaseDurationMs = firstStep.durationMs,
            prepRemainingMs = preparationDurationMs,
            confirmedActiveMs = 0L,
            totalActiveDurationMs = totalMs,
            remainingActiveMs = totalMs,
            snapshotElapsedRealtimeMs = now,
            isStopDialogVisible = false,
            interruptionReason = null,
            lastSchedulerDriftMs = 0L,
            maxSchedulerDriftMs = 0L,
            isDemo = isDemo,
            isFreeMode = false,
            isCustomProgram = true,
            currentBlockName = firstStep.blockName,
            currentBlockIndex = 1,
            totalBlocks = program.blocks.size
        )

        onLog(
            "STATE",
            "Запуск конструктора: «${program.title}» (${program.blocks.size} блоков, ${steps.size} шагов, $totalMs мс)"
        )
        onCheckpoint(snapshot, CheckpointTrigger.START)
        emitSignal(SignalType.PREP_TICK)
        return true
    }

    /**
     * Восстановление прерванной попытки (например, после смерти процесса или перезагрузки)
     * строго по последней подтверждённой контрольной точке без начисления времени простоя.
     */
    fun restoreInterruptedFromCheckpoint(
        runId: String,
        slotId: String,
        startDate: String,
        zoneId: String,
        protocol: DemoProtocolConfig,
        soundPackSnapshot: SoundPack,
        metronomeStyleSnapshot: MetronomeTickStyle,
        confirmedActiveMs: Long,
        currentRepetition: Int,
        currentPhase: PhaseType,
        phaseRemainingMs: Long,
        interruptionReason: String,
        isDemo: Boolean = true,
        isFreeMode: Boolean = false
    ): PhaseEngineSnapshot {
        val now = clock.elapsedRealtimeMs()
        lastTickRealtimeMs = now
        customExecutionSteps = emptyList()
        currentCustomStepIndex = 0
        val clampedConfirmed = confirmedActiveMs.coerceIn(0L, protocol.totalActiveDurationMs)
        lastCheckpointActiveMs = clampedConfirmed
        val phaseDur = if (currentPhase == PhaseType.CONTRACTION) {
            protocol.contractionMs
        } else {
            protocol.relaxationMs
        }
        val clampedPhaseRem = phaseRemainingMs.coerceIn(1L, phaseDur)

        snapshot = PhaseEngineSnapshot(
            runId = runId,
            slotId = slotId,
            startDate = startDate,
            zoneId = zoneId,
            status = RunStatus.INTERRUPTED,
            protocol = protocol,
            soundPackSnapshot = soundPackSnapshot,
            metronomeStyleSnapshot = metronomeStyleSnapshot,
            currentRepetition = currentRepetition.coerceIn(1, protocol.repetitions),
            totalRepetitions = protocol.repetitions,
            currentPhase = currentPhase,
            phaseRemainingMs = clampedPhaseRem,
            phaseDurationMs = phaseDur,
            prepRemainingMs = 0L,
            confirmedActiveMs = clampedConfirmed,
            totalActiveDurationMs = protocol.totalActiveDurationMs,
            remainingActiveMs = (protocol.totalActiveDurationMs - clampedConfirmed).coerceAtLeast(0L),
            snapshotElapsedRealtimeMs = now,
            isStopDialogVisible = false,
            interruptionReason = interruptionReason,
            isDemo = isDemo,
            isFreeMode = isFreeMode
        )
        onLog(
            "RECOVERY",
            "Восстановлена прерванная попытка $runId в состоянии INTERRUPTED " +
                "(активное время=${clampedConfirmed} мс, повтор ${snapshot.currentRepetition}/${snapshot.totalRepetitions}, " +
                "остаток фазы=${clampedPhaseRem} мс, без начисления простоя)"
        )
        onCheckpoint(snapshot, CheckpointTrigger.INTERRUPT)
        return snapshot
    }

    /**
     * Обновление по монотонным часам.
     */
    fun tick(): PhaseEngineSnapshot {
        val status = snapshot.status
        if (status != RunStatus.PREPARING && status != RunStatus.RUNNING) {
            return snapshot
        }

        val now = clock.elapsedRealtimeMs()
        val deltaMs = (now - lastTickRealtimeMs).coerceAtLeast(0L)
        lastTickRealtimeMs = now

        if (deltaMs == 0L) return snapshot

        val driftMs = (deltaMs - expectedTickIntervalMs).coerceAtLeast(0L)
        val updatedMaxDrift = maxOf(snapshot.maxSchedulerDriftMs, driftMs)

        // Защита от существенной задержки планировщика ОС (ТЗ п. 7):
        // не проигрываем пропущенные сигналы пачкой и не засчитываем время молча.
        if (deltaMs > schedulerDelayThresholdMs) {
            val reason = "Задержка планировщика ОС: ${deltaMs} мс (порог ${schedulerDelayThresholdMs} мс)"
            snapshot = snapshot.copy(
                status = RunStatus.INTERRUPTED,
                prepRemainingMs = 0L,
                snapshotElapsedRealtimeMs = now,
                interruptionReason = reason,
                lastSchedulerDriftMs = driftMs,
                maxSchedulerDriftMs = updatedMaxDrift
            )
            onLog("SCHEDULER", "Превышен порог задержки ($deltaMs мс). Переход в INTERRUPTED без пачки сигналов.")
            onCheckpoint(snapshot, CheckpointTrigger.INTERRUPT)
            emitSignal(SignalType.PAUSED)
            return snapshot
        }

        snapshot = snapshot.copy(
            snapshotElapsedRealtimeMs = now,
            lastSchedulerDriftMs = driftMs,
            maxSchedulerDriftMs = updatedMaxDrift
        )

        if (status == RunStatus.PREPARING) {
            advancePreparing(deltaMs, now)
        } else if (status == RunStatus.RUNNING) {
            advanceRunning(deltaMs, now)
        }

        return snapshot
    }

    private fun advancePreparing(deltaMs: Long, now: Long) {
        val remaining = snapshot.prepRemainingMs - deltaMs
        if (remaining > 0L) {
            val ceilSeconds = ((remaining + 999L) / 1000L).toInt()
            if (ceilSeconds in 1..2 && ceilSeconds < lastPrepSecondEmitted) {
                lastPrepSecondEmitted = ceilSeconds
                val sigIndex = 4 - ceilSeconds
                onLog("SIGNAL", "Подготовка: сигнал $sigIndex/3 (осталось ${remaining} мс)")
                emitSignal(SignalType.PREP_TICK)
            }
            snapshot = snapshot.copy(
                prepRemainingMs = remaining,
                snapshotElapsedRealtimeMs = now
            )
        } else {
            val overflowMs = (-remaining).coerceAtLeast(0L)
            val phaseSignal = if (snapshot.currentPhase == PhaseType.CONTRACTION) {
                SignalType.CONTRACTION
            } else {
                SignalType.RELAXATION
            }
            val currentCeilSec = ((snapshot.phaseRemainingMs + 999L) / 1000L).toInt()
            lastMetronomeSecondEmitted = currentCeilSec

            snapshot = snapshot.copy(
                status = RunStatus.RUNNING,
                prepRemainingMs = 0L,
                snapshotElapsedRealtimeMs = now,
                interruptionReason = null
            )
            onLog(
                "STATE",
                "PREPARING -> RUNNING (Повтор ${snapshot.currentRepetition}/${snapshot.totalRepetitions}, " +
                    "фаза: ${snapshot.currentPhase.labelRu}, остаток фазы: ${snapshot.phaseRemainingMs} мс)"
            )
            onCheckpoint(snapshot, CheckpointTrigger.PHASE_CHANGE)
            emitSignal(phaseSignal)

            if (overflowMs > 0L) {
                advanceRunning(overflowMs, now)
            }
        }
    }

    private fun advanceRunning(deltaMs: Long, now: Long) {
        var remainingToConsume = deltaMs
        var commandEmittedOnThisTick = false
        var phaseChangedOnThisTick = false

        while (remainingToConsume > 0L && snapshot.status == RunStatus.RUNNING) {
            val step = minOf(remainingToConsume, snapshot.phaseRemainingMs)
            remainingToConsume -= step

            val newPhaseRemaining = snapshot.phaseRemainingMs - step
            val newConfirmedActive = (snapshot.confirmedActiveMs + step)
                .coerceAtMost(snapshot.totalActiveDurationMs)
            val newRemainingActive = (snapshot.totalActiveDurationMs - newConfirmedActive)
                .coerceAtLeast(0L)

            if (newPhaseRemaining > 0L) {
                snapshot = snapshot.copy(
                    phaseRemainingMs = newPhaseRemaining,
                    confirmedActiveMs = newConfirmedActive,
                    remainingActiveMs = newRemainingActive,
                    snapshotElapsedRealtimeMs = now
                )
            } else {
                commandEmittedOnThisTick = true
                phaseChangedOnThisTick = true

                if (customExecutionSteps.isNotEmpty()) {
                    if (currentCustomStepIndex + 1 < customExecutionSteps.size) {
                        currentCustomStepIndex++
                        val nextStep = customExecutionSteps[currentCustomStepIndex]
                        val sig = if (nextStep.phaseType == PhaseType.CONTRACTION) {
                            SignalType.CONTRACTION
                        } else {
                            SignalType.RELAXATION
                        }
                        lastMetronomeSecondEmitted = ((nextStep.durationMs + 999L) / 1000L).toInt()
                        snapshot = snapshot.copy(
                            currentPhase = nextStep.phaseType,
                            phaseRemainingMs = nextStep.durationMs,
                            phaseDurationMs = nextStep.durationMs,
                            currentRepetition = nextStep.cycleIndex,
                            totalRepetitions = nextStep.totalCyclesInBlock,
                            currentBlockName = nextStep.blockName,
                            confirmedActiveMs = newConfirmedActive,
                            remainingActiveMs = newRemainingActive,
                            snapshotElapsedRealtimeMs = now
                        )
                        lastCheckpointActiveMs = newConfirmedActive
                        onLog(
                            "PHASE",
                            "Конструктор: шаг ${currentCustomStepIndex + 1}/${customExecutionSteps.size} «${nextStep.blockName}» -> ${nextStep.phaseType.labelRu} (${nextStep.durationMs / 1000}с)"
                        )
                        onCheckpoint(snapshot, CheckpointTrigger.PHASE_CHANGE)
                        emitSignal(sig)
                    } else {
                        snapshot = snapshot.copy(
                            status = RunStatus.COMPLETED,
                            phaseRemainingMs = 0L,
                            confirmedActiveMs = snapshot.totalActiveDurationMs,
                            remainingActiveMs = 0L,
                            snapshotElapsedRealtimeMs = now,
                            isStopDialogVisible = false
                        )
                        lastCheckpointActiveMs = snapshot.totalActiveDurationMs
                        onLog("STATE", "Конструктор: программа успешно завершена (${snapshot.totalActiveDurationMs} мс)")
                        onCheckpoint(snapshot, CheckpointTrigger.COMPLETE)
                        emitSignal(SignalType.SET_COMPLETED)
                    }
                } else if (snapshot.currentPhase == PhaseType.CONTRACTION) {
                    val relaxDuration = snapshot.protocol.relaxationMs
                    lastMetronomeSecondEmitted = ((relaxDuration + 999L) / 1000L).toInt()
                    snapshot = snapshot.copy(
                        currentPhase = PhaseType.RELAXATION,
                        phaseRemainingMs = relaxDuration,
                        phaseDurationMs = relaxDuration,
                        confirmedActiveMs = newConfirmedActive,
                        remainingActiveMs = newRemainingActive,
                        snapshotElapsedRealtimeMs = now
                    )
                    lastCheckpointActiveMs = newConfirmedActive
                    onLog(
                        "PHASE",
                        "Повтор ${snapshot.currentRepetition}/${snapshot.totalRepetitions}: Сокращение -> Расслабление"
                    )
                    onCheckpoint(snapshot, CheckpointTrigger.PHASE_CHANGE)
                    emitSignal(SignalType.RELAXATION)
                } else {
                    if (snapshot.currentRepetition < snapshot.totalRepetitions) {
                        val nextRep = snapshot.currentRepetition + 1
                        val contractDuration = snapshot.protocol.contractionMs
                        lastMetronomeSecondEmitted = ((contractDuration + 999L) / 1000L).toInt()
                        snapshot = snapshot.copy(
                            currentRepetition = nextRep,
                            currentPhase = PhaseType.CONTRACTION,
                            phaseRemainingMs = contractDuration,
                            phaseDurationMs = contractDuration,
                            confirmedActiveMs = newConfirmedActive,
                            remainingActiveMs = newRemainingActive,
                            snapshotElapsedRealtimeMs = now
                        )
                        lastCheckpointActiveMs = newConfirmedActive
                        onLog(
                            "PHASE",
                            "Начат повтор $nextRep/${snapshot.totalRepetitions}: фаза Сокращение"
                        )
                        onCheckpoint(snapshot, CheckpointTrigger.PHASE_CHANGE)
                        emitSignal(SignalType.CONTRACTION)
                    } else {
                        val finalSignal = if (snapshot.protocol.isLastInGroup) {
                            SignalType.GROUP_COMPLETED
                        } else {
                            SignalType.SET_COMPLETED
                        }
                        snapshot = snapshot.copy(
                            status = RunStatus.COMPLETED,
                            phaseRemainingMs = 0L,
                            confirmedActiveMs = snapshot.totalActiveDurationMs,
                            remainingActiveMs = 0L,
                            snapshotElapsedRealtimeMs = now,
                            isStopDialogVisible = false
                        )
                        lastCheckpointActiveMs = snapshot.totalActiveDurationMs
                        onLog(
                            "STATE",
                            "RUNNING -> COMPLETED (активное время ${snapshot.totalActiveDurationMs} мс, сигнал: ${finalSignal.labelRu})"
                        )
                        onCheckpoint(snapshot, CheckpointTrigger.COMPLETE)
                        emitSignal(finalSignal)
                    }
                }
            }
        }

        // Периодическая контрольная точка каждые ~5 секунд активной работы (НЕ на каждом кадре)
        if (snapshot.status == RunStatus.RUNNING &&
            !phaseChangedOnThisTick &&
            snapshot.confirmedActiveMs - lastCheckpointActiveMs >= periodicCheckpointIntervalMs
        ) {
            lastCheckpointActiveMs = snapshot.confirmedActiveMs
            onLog("CHECKPOINT", "Периодическая контрольная точка: ${snapshot.confirmedActiveMs} мс активной работы")
            onCheckpoint(snapshot, CheckpointTrigger.PERIODIC_5S)
        }

        // Тихий метроном внутри фазы: если фаза продолжается и на этом тике не подавалась основная команда
        if (snapshot.status == RunStatus.RUNNING && isMetronomeEnabled && !commandEmittedOnThisTick) {
            val ceilSeconds = ((snapshot.phaseRemainingMs + 999L) / 1000L).toInt()
            val totalPhaseSeconds = ((snapshot.phaseDurationMs + 999L) / 1000L).toInt()
            if (ceilSeconds in 1 until totalPhaseSeconds && ceilSeconds < lastMetronomeSecondEmitted) {
                lastMetronomeSecondEmitted = ceilSeconds
                onLog("METRONOME", "Тик метронома (осталось $ceilSeconds сек фазы)")
                emitSignal(SignalType.METRONOME_TICK)
            }
        }
    }

    /**
     * Ручная пауза: немедленно останавливает активное время и метроном, сообщает «Пауза — расслабьтесь».
     */
    fun pause(reason: String = "Пауза нажата пользователем"): PhaseEngineSnapshot {
        if (snapshot.status != RunStatus.PREPARING && snapshot.status != RunStatus.RUNNING) {
            return snapshot
        }
        val now = clock.elapsedRealtimeMs()
        val delta = (now - lastTickRealtimeMs).coerceAtLeast(0L)
        if (delta in 1..schedulerDelayThresholdMs && snapshot.status == RunStatus.RUNNING) {
            val step = minOf(delta, snapshot.phaseRemainingMs - 1L).coerceAtLeast(0L)
            val newConfirmed = (snapshot.confirmedActiveMs + step).coerceAtMost(snapshot.totalActiveDurationMs)
            snapshot = snapshot.copy(
                phaseRemainingMs = snapshot.phaseRemainingMs - step,
                confirmedActiveMs = newConfirmed,
                remainingActiveMs = (snapshot.totalActiveDurationMs - newConfirmed).coerceAtLeast(0L)
            )
        }
        lastTickRealtimeMs = now

        snapshot = snapshot.copy(
            status = RunStatus.PAUSED,
            prepRemainingMs = 0L,
            snapshotElapsedRealtimeMs = now,
            interruptionReason = reason
        )
        lastCheckpointActiveMs = snapshot.confirmedActiveMs
        onLog("STATE", "Переход в PAUSED ($reason). Команда: Пауза — расслабьтесь")
        onCheckpoint(snapshot, CheckpointTrigger.PAUSE)
        emitSignal(SignalType.PAUSED)
        return snapshot
    }

    /**
     * Внешнее прерывание (потеря аудиофокуса, звонок, отключение наушников, смерть процесса).
     */
    fun interrupt(reason: String): PhaseEngineSnapshot {
        if (snapshot.status != RunStatus.PREPARING && snapshot.status != RunStatus.RUNNING) {
            return snapshot
        }
        val now = clock.elapsedRealtimeMs()
        lastTickRealtimeMs = now
        snapshot = snapshot.copy(
            status = RunStatus.INTERRUPTED,
            prepRemainingMs = 0L,
            snapshotElapsedRealtimeMs = now,
            interruptionReason = reason
        )
        lastCheckpointActiveMs = snapshot.confirmedActiveMs
        onLog("INTERRUPT", "Прерывание: $reason -> состояние INTERRUPTED (без автовозобновления)")
        onCheckpoint(snapshot, CheckpointTrigger.INTERRUPT)
        emitSignal(SignalType.PAUSED)
        return snapshot
    }

    /**
     * Ручное продолжение с паузы или прерывания:
     * 3 коротких сигнала подготовки, затем остаток текущей фазы.
     */
    fun resume(): Boolean {
        if (snapshot.status != RunStatus.PAUSED && snapshot.status != RunStatus.INTERRUPTED) {
            return false
        }
        val now = clock.elapsedRealtimeMs()
        lastTickRealtimeMs = now
        lastPrepSecondEmitted = 3
        snapshot = snapshot.copy(
            status = RunStatus.PREPARING,
            prepRemainingMs = preparationDurationMs,
            snapshotElapsedRealtimeMs = now,
            isStopDialogVisible = false,
            interruptionReason = null
        )
        onLog(
            "STATE",
            "Возобновление вручную -> PREPARING (3 сигнала), далее остаток фазы " +
                "${snapshot.currentPhase.labelRu} (${snapshot.phaseRemainingMs} мс)"
        )
        onCheckpoint(snapshot, CheckpointTrigger.RESUME)
        emitSignal(SignalType.PREP_TICK)
        return true
    }

    /**
     * Запрос остановки (ТЗ п. 7): сначала переводит в паузу, затем открывает диалог «Завершить без зачёта?».
     */
    fun requestStop(): PhaseEngineSnapshot {
        if (snapshot.status == RunStatus.PREPARING || snapshot.status == RunStatus.RUNNING) {
            pause(reason = "Пауза при нажатии «Остановить»")
        }
        if (snapshot.status == RunStatus.PAUSED || snapshot.status == RunStatus.INTERRUPTED) {
            snapshot = snapshot.copy(isStopDialogVisible = true)
            onLog("DIALOG", "Открыт диалог «Завершить без зачёта?» (состояние удерживается на паузе)")
        }
        return snapshot
    }

    /**
     * Отмена диалога остановки («Остаться на паузе» или Back) оставляет паузу.
     */
    fun cancelStopDialog(): PhaseEngineSnapshot {
        if (snapshot.isStopDialogVisible) {
            snapshot = snapshot.copy(isStopDialogVisible = false)
            onLog("DIALOG", "Выбрано «Остаться на паузе» — пауза сохранена")
        }
        return snapshot
    }

    /**
     * Подтверждение «Завершить» в диалоге остановки: переводит в STOPPED без зачёта.
     */
    fun confirmStop(): PhaseEngineSnapshot {
        if (!snapshot.isStopDialogVisible ||
            snapshot.status !in listOf(RunStatus.PAUSED, RunStatus.INTERRUPTED)) return snapshot
        val now = clock.elapsedRealtimeMs()
        snapshot = snapshot.copy(
            status = RunStatus.STOPPED,
            prepRemainingMs = 0L,
            snapshotElapsedRealtimeMs = now,
            isStopDialogVisible = false
        )
        onLog("STATE", "Попытка остановлена без зачёта (STOPPED)")
        onCheckpoint(snapshot, CheckpointTrigger.STOP)
        return snapshot
    }

    fun resetToIdle(): PhaseEngineSnapshot {
        customExecutionSteps = emptyList()
        currentCustomStepIndex = 0
        val now = clock.elapsedRealtimeMs()
        val protocol = snapshot.protocol
        snapshot = PhaseEngineSnapshot(
            status = RunStatus.IDLE,
            protocol = protocol,
            currentRepetition = 1,
            totalRepetitions = protocol.repetitions,
            currentPhase = PhaseType.CONTRACTION,
            phaseRemainingMs = protocol.contractionMs,
            phaseDurationMs = protocol.contractionMs,
            prepRemainingMs = 0L,
            confirmedActiveMs = 0L,
            totalActiveDurationMs = protocol.totalActiveDurationMs,
            remainingActiveMs = protocol.totalActiveDurationMs,
            snapshotElapsedRealtimeMs = now
        )
        onLog("STATE", "Сброс в состояние IDLE")
        return snapshot
    }

    private fun emitSignal(signal: SignalType) {
        onSignal(signal, snapshot.soundPackSnapshot, snapshot.metronomeStyleSnapshot)
    }
}
