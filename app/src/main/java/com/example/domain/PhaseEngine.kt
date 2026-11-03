package com.example.domain

/**
 * Чистый Kotlin-движок фаз интервального таймера (тестируется без экрана и без Android-контекста).
 * Реализует правила разделов 7 и 8 ТЗ v1.0:
 * - Состояния: IDLE, PREPARING, RUNNING, PAUSED, INTERRUPTED, COMPLETED, STOPPED.
 * - Максимум одна активная попытка.
 * - Подготовка 3 секунды (3 коротких сигнала), не входит в активное время.
 * - При совпадении тика метронома и команды фазы тик пропускается.
 * - На паузе всегда отображается «Пауза — расслабьтесь».
 * - Продолжение вручную: 3 сигнала подготовки, затем остаток прерванной фазы.
 * - Остановка: сначала пауза, затем диалог «Завершить без зачёта?»; отмена диалога оставляет паузу.
 * - Защита от задержки планировщика ОС: если тик задержан сильнее порога, переход в INTERRUPTED без пачки сигналов.
 */
class PhaseEngine(
    private val clock: MonotonicClock,
    private val onSignal: (SignalType) -> Unit = {},
    private val onLog: (category: String, message: String) -> Unit = { _, _ -> },
    val preparationDurationMs: Long = 3_000L,
    val expectedTickIntervalMs: Long = 100L,
    val schedulerDelayThresholdMs: Long = 2_500L
) {
    private var snapshot = PhaseEngineSnapshot()
    private var lastTickRealtimeMs: Long = 0L
    private var lastPrepSecondEmitted: Int = -1
    private var lastMetronomeSecondEmitted: Int = -1
    var isMetronomeEnabled: Boolean = true

    fun currentSnapshot(): PhaseEngineSnapshot = snapshot

    fun selectProtocolIfIdle(protocol: DemoProtocolConfig): Boolean {
        if (snapshot.isActiveOrPaused) {
            onLog("ENGINE", "Отклонена смена протокола во время активной попытки (${snapshot.status})")
            return false
        }
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
            remainingActiveMs = protocol.totalActiveDurationMs
        )
        onLog("PROTOCOL", "Выбран тестовый протокол: ${protocol.titleRu}")
        return true
    }

    /**
     * Запуск новой демонстрационной попытки.
     * Если уже есть активная или приостановленная попытка, повторный старт блокируется.
     */
    fun start(protocol: DemoProtocolConfig = snapshot.protocol): Boolean {
        if (snapshot.isActiveOrPaused) {
            onLog("GUARD", "Повторный быстрый старт заблокирован: уже активно состояние ${snapshot.status}")
            return false
        }
        val now = clock.elapsedRealtimeMs()
        lastTickRealtimeMs = now
        lastPrepSecondEmitted = 3
        lastMetronomeSecondEmitted = -1

        snapshot = PhaseEngineSnapshot(
            status = RunStatus.PREPARING,
            protocol = protocol,
            currentRepetition = 1,
            totalRepetitions = protocol.repetitions,
            currentPhase = PhaseType.CONTRACTION,
            phaseRemainingMs = protocol.contractionMs,
            phaseDurationMs = protocol.contractionMs,
            prepRemainingMs = preparationDurationMs,
            confirmedActiveMs = 0L,
            totalActiveDurationMs = protocol.totalActiveDurationMs,
            remainingActiveMs = protocol.totalActiveDurationMs,
            isStopDialogVisible = false,
            interruptionReason = null,
            lastSchedulerDriftMs = 0L,
            maxSchedulerDriftMs = 0L,
            isDemo = true
        )
        onLog("STATE", "IDLE -> PREPARING (подготовка 3 сек, сигнал 1/3)")
        onSignal(SignalType.PREP_TICK)
        return true
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
                interruptionReason = reason,
                lastSchedulerDriftMs = driftMs,
                maxSchedulerDriftMs = updatedMaxDrift
            )
            onLog("SCHEDULER", "Превышен порог задержки ($deltaMs мс). Переход в INTERRUPTED без пачки сигналов.")
            onSignal(SignalType.PAUSED)
            return snapshot
        }

        snapshot = snapshot.copy(
            lastSchedulerDriftMs = driftMs,
            maxSchedulerDriftMs = updatedMaxDrift
        )

        if (status == RunStatus.PREPARING) {
            advancePreparing(deltaMs)
        } else if (status == RunStatus.RUNNING) {
            advanceRunning(deltaMs)
        }

        return snapshot
    }

    private fun advancePreparing(deltaMs: Long) {
        val remaining = snapshot.prepRemainingMs - deltaMs
        if (remaining > 0L) {
            // Подаём короткие сигналы на границах 2 сек и 1 сек (всего 3 сигнала подготовки)
            val ceilSeconds = ((remaining + 999L) / 1000L).toInt()
            if (ceilSeconds in 1..2 && ceilSeconds < lastPrepSecondEmitted) {
                lastPrepSecondEmitted = ceilSeconds
                val sigIndex = 4 - ceilSeconds
                onLog("SIGNAL", "Подготовка: сигнал $sigIndex/3 (осталось ${remaining} мс)")
                onSignal(SignalType.PREP_TICK)
            }
            snapshot = snapshot.copy(prepRemainingMs = remaining)
        } else {
            // Подготовка завершена -> переходим в RUNNING и явно подаём сигнал текущей фазы
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
                interruptionReason = null
            )
            onLog(
                "STATE",
                "PREPARING -> RUNNING (Повтор ${snapshot.currentRepetition}/${snapshot.totalRepetitions}, " +
                    "фаза: ${snapshot.currentPhase.labelRu}, остаток фазы: ${snapshot.phaseRemainingMs} мс)"
            )
            onSignal(phaseSignal)

            if (overflowMs > 0L) {
                advanceRunning(overflowMs)
            }
        }
    }

    private fun advanceRunning(deltaMs: Long) {
        var remainingToConsume = deltaMs
        var commandEmittedOnThisTick = false

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
                    remainingActiveMs = newRemainingActive
                )
            } else {
                // Текущая фаза завершилась
                commandEmittedOnThisTick = true
                if (snapshot.currentPhase == PhaseType.CONTRACTION) {
                    val relaxDuration = snapshot.protocol.relaxationMs
                    lastMetronomeSecondEmitted = ((relaxDuration + 999L) / 1000L).toInt()
                    snapshot = snapshot.copy(
                        currentPhase = PhaseType.RELAXATION,
                        phaseRemainingMs = relaxDuration,
                        phaseDurationMs = relaxDuration,
                        confirmedActiveMs = newConfirmedActive,
                        remainingActiveMs = newRemainingActive
                    )
                    onLog(
                        "PHASE",
                        "Повтор ${snapshot.currentRepetition}/${snapshot.totalRepetitions}: Сокращение -> Расслабление"
                    )
                    onSignal(SignalType.RELAXATION)
                } else {
                    // Завершилось расслабление
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
                            remainingActiveMs = newRemainingActive
                        )
                        onLog(
                            "PHASE",
                            "Начат повтор $nextRep/${snapshot.totalRepetitions}: фаза Сокращение"
                        )
                        onSignal(SignalType.CONTRACTION)
                    } else {
                        // Весь подход завершён!
                        // ТЗ п. 8: на последнем упражнении группы финальный сигнал заменяет обычный сигнал подхода; двойного финала нет.
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
                            isStopDialogVisible = false
                        )
                        onLog(
                            "STATE",
                            "RUNNING -> COMPLETED (активное время ${snapshot.totalActiveDurationMs} мс, сигнал: ${finalSignal.labelRu})"
                        )
                        onSignal(finalSignal)
                    }
                }
            }
        }

        // Тихий метроном внутри фазы: если фаза продолжается и на этом тике не подавалась основная команда
        if (snapshot.status == RunStatus.RUNNING && isMetronomeEnabled && !commandEmittedOnThisTick) {
            val ceilSeconds = ((snapshot.phaseRemainingMs + 999L) / 1000L).toInt()
            val totalPhaseSeconds = ((snapshot.phaseDurationMs + 999L) / 1000L).toInt()
            if (ceilSeconds in 1 until totalPhaseSeconds && ceilSeconds < lastMetronomeSecondEmitted) {
                lastMetronomeSecondEmitted = ceilSeconds
                onLog("METRONOME", "Тик метронома (осталось $ceilSeconds сек фазы)")
                onSignal(SignalType.METRONOME_TICK)
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
        // Вычитываем время, прошедшее до момента нажатия паузы, если не превышен порог задержки
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
            interruptionReason = reason
        )
        onLog("STATE", "Переход в PAUSED ($reason). Команда: Пауза — расслабьтесь")
        onSignal(SignalType.PAUSED)
        return snapshot
    }

    /**
     * Внешнее прерывание (потеря аудиофокуса, звонок, отключение наушников, смерть процесса).
     * Переводит в INTERRUPTED / паузу без автовозобновления.
     */
    fun interrupt(reason: String): PhaseEngineSnapshot {
        if (snapshot.status != RunStatus.PREPARING && snapshot.status != RunStatus.RUNNING) {
            return snapshot
        }
        lastTickRealtimeMs = clock.elapsedRealtimeMs()
        snapshot = snapshot.copy(
            status = RunStatus.INTERRUPTED,
            prepRemainingMs = 0L,
            interruptionReason = reason
        )
        onLog("INTERRUPT", "Прерывание: $reason -> состояние INTERRUPTED (без автовозобновления)")
        onSignal(SignalType.PAUSED)
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
        lastTickRealtimeMs = clock.elapsedRealtimeMs()
        lastPrepSecondEmitted = 3
        snapshot = snapshot.copy(
            status = RunStatus.PREPARING,
            prepRemainingMs = preparationDurationMs,
            isStopDialogVisible = false,
            interruptionReason = null
        )
        onLog(
            "STATE",
            "Возобновление вручную -> PREPARING (3 сигнала), далее остаток фазы " +
                "${snapshot.currentPhase.labelRu} (${snapshot.phaseRemainingMs} мс)"
        )
        onSignal(SignalType.PREP_TICK)
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
     * Отмена диалога остановки («Остаться на паузе» или системная кнопка Back) оставляет паузу.
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
        snapshot = snapshot.copy(
            status = RunStatus.STOPPED,
            prepRemainingMs = 0L,
            isStopDialogVisible = false
        )
        onLog("STATE", "Попытка остановлена без зачёта (STOPPED)")
        return snapshot
    }

    fun resetToIdle(): PhaseEngineSnapshot {
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
            remainingActiveMs = protocol.totalActiveDurationMs
        )
        onLog("STATE", "Сброс стенда в состояние IDLE")
        return snapshot
    }
}
