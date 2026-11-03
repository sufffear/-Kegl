package com.example.domain

/**
 * Состояния машины состояний подхода согласно разделу 7 ТЗ v1.0.
 */
enum class RunStatus(val labelRu: String) {
    IDLE("Ожидание старта"),
    PREPARING("Подготовка (3 сигнала)"),
    RUNNING("Активная фаза"),
    PAUSED("Пауза — расслабьтесь"),
    INTERRUPTED("Прервано — расслабьтесь"),
    COMPLETED("Подход завершён"),
    STOPPED("Остановлено без зачёта")
}

/**
 * Рабочие фазы внутри состояния RUNNING.
 */
enum class PhaseType(val labelRu: String) {
    CONTRACTION("Сокращение"),
    RELAXATION("Расслабление")
}

/**
 * Типы подаваемых сигналов (раздел 8 ТЗ v1.0).
 */
enum class SignalType(val labelRu: String) {
    PREP_TICK("Сигнал подготовки (отсчёт)"),
    CONTRACTION("Команда: Сокращение"),
    RELAXATION("Команда: Расслабление"),
    METRONOME_TICK("Тик метронома"),
    PAUSED("Пауза — расслабьтесь"),
    SET_COMPLETED("Сигнал: Подход завершён"),
    GROUP_COMPLETED("Сигнал: Группа завершена")
}

/**
 * Конфигурация демонстрационного тестового протокола для стенда 0B.
 * Включает техническую валидацию параметров по разделу 2 ТЗ v1.0.
 */
data class DemoProtocolConfig(
    val id: String,
    val titleRu: String,
    val contractionMs: Long,
    val relaxationMs: Long,
    val repetitions: Int,
    val isLastInGroup: Boolean = false
) {
    init {
        require(contractionMs in 1_000L..60_000L) {
            "Длительность фазы сокращения должна быть положительной (1..60 сек)"
        }
        require(relaxationMs in 1_000L..60_000L) {
            "Длительность фазы расслабления должна быть положительной (1..60 сек)"
        }
        require(repetitions in 1..50) {
            "Число повторов должно быть целым положительным числом (1..50)"
        }
    }

    val singleRepDurationMs: Long
        get() = contractionMs + relaxationMs

    val totalActiveDurationMs: Long
        get() = singleRepDurationMs * repetitions
}

/**
 * Снимок состояния движка фаз.
 */
data class PhaseEngineSnapshot(
    val status: RunStatus = RunStatus.IDLE,
    val protocol: DemoProtocolConfig = DEFAULT_DEMO_PROTOCOLS.first(),
    val currentRepetition: Int = 1,
    val totalRepetitions: Int = protocol.repetitions,
    val currentPhase: PhaseType = PhaseType.CONTRACTION,
    val phaseRemainingMs: Long = protocol.contractionMs,
    val phaseDurationMs: Long = protocol.contractionMs,
    val prepRemainingMs: Long = 0L,
    val confirmedActiveMs: Long = 0L,
    val totalActiveDurationMs: Long = protocol.totalActiveDurationMs,
    val remainingActiveMs: Long = protocol.totalActiveDurationMs,
    val isStopDialogVisible: Boolean = false,
    val interruptionReason: String? = null,
    val lastSchedulerDriftMs: Long = 0L,
    val maxSchedulerDriftMs: Long = 0L,
    val isDemo: Boolean = true
) {
    val isActiveOrPaused: Boolean
        get() = status == RunStatus.PREPARING ||
            status == RunStatus.RUNNING ||
            status == RunStatus.PAUSED ||
            status == RunStatus.INTERRUPTED

    /**
     * На паузе или при прерывании никогда не оставляем команду сокращения как актуальную (ТЗ п. 7).
     */
    val headlineCommandRu: String
        get() = when (status) {
            RunStatus.IDLE -> "Готов к проверке (Демо)"
            RunStatus.PREPARING -> "Подготовка… Расслабьтесь"
            RunStatus.RUNNING -> currentPhase.labelRu
            RunStatus.PAUSED -> "Пауза — расслабьтесь"
            RunStatus.INTERRUPTED -> "Пауза (прервано) — расслабьтесь"
            RunStatus.COMPLETED -> "Подход завершён (Демо)"
            RunStatus.STOPPED -> "Остановлено без зачёта"
        }
}

/**
 * Источник монотонного времени (не зависит от перевода системных часов).
 */
fun interface MonotonicClock {
    fun elapsedRealtimeMs(): Long
}

/**
 * Запись технического лога событий этапа 0B (без личных данных).
 */
data class BenchEventLogEntry(
    val id: Long,
    val elapsedMs: Long,
    val category: String,
    val message: String
)

val DEFAULT_DEMO_PROTOCOLS = listOf(
    DemoProtocolConfig(
        id = "demo_short_3x4",
        titleRu = "Демо-тест 1: 3 повтора (4 сек / 4 сек)",
        contractionMs = 4_000L,
        relaxationMs = 4_000L,
        repetitions = 3,
        isLastInGroup = false
    ),
    DemoProtocolConfig(
        id = "demo_group_final_2x3",
        titleRu = "Демо-тест 2 (финал группы): 2 повтора (3 сек / 3 сек)",
        contractionMs = 3_000L,
        relaxationMs = 3_000L,
        repetitions = 2,
        isLastInGroup = true
    ),
    DemoProtocolConfig(
        id = "demo_screen_off_5x5",
        titleRu = "Демо-тест 3 (для проверки экрана): 5 повторов (5 сек / 5 сек)",
        contractionMs = 5_000L,
        relaxationMs = 5_000L,
        repetitions = 5,
        isLastInGroup = false
    )
)
