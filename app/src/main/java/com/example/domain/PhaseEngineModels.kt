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
 * Рабочие фазы внутри состояния RUNNING (Итерация v1.4 п. 6: «Удержание» / «Отдых»).
 */
enum class PhaseType(val labelRu: String) {
    CONTRACTION("Удержание"),
    RELAXATION("Отдых")
}

/**
 * Типы подаваемых сигналов (раздел 8 ТЗ v1.0).
 */
enum class SignalType(val labelRu: String) {
    PREP_TICK("Сигнал подготовки (отсчёт)"),
    CONTRACTION("Команда: Удержание"),
    RELAXATION("Команда: Отдых"),
    METRONOME_TICK("Тик метронома"),
    PAUSED("Пауза — расслабьтесь"),
    SET_COMPLETED("Сигнал: Тренировка завершена"),
    GROUP_COMPLETED("Сигнал: Группа завершена")
}

/**
 * Три локальных офлайн-набора звуков согласно заданию:
 * - «Мягкий» — спокойные приглушённые тоны;
 * - «Колокольчики» — короткие чистые переливы;
 * - «Чёткий» — отчётливо различимые короткие сигналы.
 */
enum class SoundPack(
    val id: String,
    val titleRu: String,
    val descriptionRu: String
) {
    SOFT(
        id = "soft",
        titleRu = "Мягкий",
        descriptionRu = "Спокойные приглушённые тоны без резких верхов"
    ),
    CHIMES(
        id = "chimes",
        titleRu = "Колокольчики",
        descriptionRu = "Короткие чистые переливы с мягким затуханием"
    ),
    CRISP(
        id = "crisp",
        titleRu = "Чёткий",
        descriptionRu = "Отчётливо различимые короткие сигналы"
    );

    companion object {
        fun fromId(id: String?): SoundPack =
            entries.firstOrNull { it.id == id } ?: SOFT
    }
}

/**
 * Два варианта тика метронома: мягкий и чёткий.
 */
enum class MetronomeTickStyle(
    val id: String,
    val titleRu: String
) {
    SOFT("soft_tick", "Мягкий"),
    CRISP("crisp_tick", "Чёткий");

    companion object {
        fun fromId(id: String?): MetronomeTickStyle =
            entries.firstOrNull { it.id == id } ?: SOFT
    }
}

/**
 * Группы дневного плана (ТЗ п. 1, п. 5).
 */
enum class PlanGroup(val code: String, val titleRu: String, val order: Int) {
    MORNING("MORNING", "Утро", 1),
    NOON("NOON", "Обед", 2),
    EVENING("EVENING", "Вечер", 3);

    companion object {
        fun fromCode(code: String): PlanGroup =
            entries.firstOrNull { it.code == code } ?: MORNING
    }
}

/**
 * Необязательная оценка после завершения подхода (ТЗ п. 5).
 * Не меняет нагрузку автоматически.
 */
enum class WorkoutDifficulty(val code: String, val labelRu: String) {
    EASY("EASY", "Легко"),
    NORMAL("NORMAL", "Нормально"),
    HARD("HARD", "Тяжело");

    companion object {
        fun fromCode(code: String?): WorkoutDifficulty? =
            entries.firstOrNull { it.code == code }
    }
}

/**
 * Конфигурация протокола подхода (снимок параметров).
 * Включает техническую валидацию параметров по разделу 2 ТЗ v1.0.
 */
data class DemoProtocolConfig(
    val id: String,
    val titleRu: String,
    val contractionMs: Long,
    val relaxationMs: Long,
    val repetitions: Int,
    val isLastInGroup: Boolean = false,
    val group: PlanGroup = PlanGroup.MORNING
) {
    init {
        require(contractionMs in 1_000L..120_000L) {
            "Длительность фазы удержания должна быть положительной (1..120 сек)"
        }
        require(relaxationMs in 1_000L..120_000L) {
            "Длительность фазы отдыха должна быть положительной (1..120 сек)"
        }
        require(repetitions in 1..300) {
            "Число циклов должно быть целым положительным числом (1..300)"
        }
    }

    val singleRepDurationMs: Long
        get() = contractionMs + relaxationMs

    val totalActiveDurationMs: Long
        get() = singleRepDurationMs * repetitions
}

/**
 * Расчёт параметров свободного интервального подхода («Мои интервалы», Итерация v1.3 / v1.4 п. 5).
 * Цикл: удержание -> отдых.
 * Подход не обрывает фазу посередине цикла.
 * Число циклов = floor(желаемое время / длительность цикла).
 * Фактическое время = число циклов * длительность цикла.
 * Ограничение 300 циклов объясняется понятным сообщением без молчаливого изменения введённых значений.
 */
data class FreeIntervalCalculation(
    val desiredTotalSec: Int,
    val cycleSec: Int,
    val cycles: Int,
    val actualTotalSec: Int,
    val isValid: Boolean,
    val errorMessageRu: String?
) {
    val discrepancySec: Int get() = (desiredTotalSec - actualTotalSec).coerceAtLeast(0)
    val completedCycles: Int get() = cycles
    val actualDurationSec: Int get() = actualTotalSec
    val unusedRemainderSec: Int get() = discrepancySec

    val summaryRu: String
        get() {
            if (!isValid) return errorMessageRu ?: "Проверьте параметры интервалов"
            val desiredMin = desiredTotalSec / 60
            val desiredSec = desiredTotalSec % 60
            val desiredStr = String.format(java.util.Locale.US, "%d:%02d", desiredMin, desiredSec)

            val actualMin = actualTotalSec / 60
            val actualSec = actualTotalSec % 60
            val actualStr = String.format(java.util.Locale.US, "%d:%02d", actualMin, actualSec)

            return if (desiredTotalSec == actualTotalSec) {
                "Выбрано $desiredStr. $cycles полных циклов, ровно $actualStr"
            } else {
                "Выбрано $desiredStr. Получится $cycles полных циклов, $actualStr"
            }
        }
}

fun calculateFreeCycles(
    desiredDurationSec: Int,
    contractionSec: Int,
    relaxationSec: Int
): FreeIntervalCalculation = calculateFreeCycles(
    targetMinutes = desiredDurationSec / 60,
    targetSeconds = desiredDurationSec % 60,
    contractionSec = contractionSec,
    relaxationSec = relaxationSec
)

fun calculateFreeCycles(
    targetMinutes: Int,
    targetSeconds: Int,
    contractionSec: Int,
    relaxationSec: Int
): FreeIntervalCalculation {
    val desiredSec = (targetMinutes * 60 + targetSeconds).coerceAtLeast(0)
    val cycleSec = contractionSec + relaxationSec
    if (contractionSec <= 0 || relaxationSec <= 0) {
        return FreeIntervalCalculation(
            desiredTotalSec = desiredSec,
            cycleSec = cycleSec,
            cycles = 0,
            actualTotalSec = 0,
            isValid = false,
            errorMessageRu = "Укажите секунды удержания и отдыха от 1 до 120 с"
        )
    }
    if (contractionSec > 120 || relaxationSec > 120) {
        return FreeIntervalCalculation(
            desiredTotalSec = desiredSec,
            cycleSec = cycleSec,
            cycles = 0,
            actualTotalSec = 0,
            isValid = false,
            errorMessageRu = "Интервал удержания или отдыха не может превышать 120 секунд"
        )
    }
    if (desiredSec <= 0) {
        return FreeIntervalCalculation(
            desiredTotalSec = desiredSec,
            cycleSec = cycleSec,
            cycles = 0,
            actualTotalSec = 0,
            isValid = false,
            errorMessageRu = "Продолжительность тренировки должна быть больше 0 с"
        )
    }
    if (desiredSec > 5400) {
        return FreeIntervalCalculation(
            desiredTotalSec = desiredSec,
            cycleSec = cycleSec,
            cycles = 0,
            actualTotalSec = 0,
            isValid = false,
            errorMessageRu = "Максимальная продолжительность тренировки — 90 минут"
        )
    }
    val rawCycles = desiredSec / cycleSec
    if (rawCycles == 0) {
        return FreeIntervalCalculation(
            desiredTotalSec = desiredSec,
            cycleSec = cycleSec,
            cycles = 0,
            actualTotalSec = 0,
            isValid = false,
            errorMessageRu = "Длительность меньше одного цикла. Увеличьте общее время или уменьшите интервалы."
        )
    }
    if (rawCycles > 300) {
        return FreeIntervalCalculation(
            desiredTotalSec = desiredSec,
            cycleSec = cycleSec,
            cycles = rawCycles,
            actualTotalSec = rawCycles * cycleSec,
            isValid = false,
            errorMessageRu = "Превышен предел 300 циклов ($rawCycles). Уменьшите общее время или увеличьте секунды удержания и отдыха."
        )
    }
    val actualSec = rawCycles * cycleSec
    return FreeIntervalCalculation(
        desiredTotalSec = desiredSec,
        cycleSec = cycleSec,
        cycles = rawCycles,
        actualTotalSec = actualSec,
        isValid = true,
        errorMessageRu = null
    )
}

/**
 * Снимок состояния движка фаз.
 * Содержит монотонную метку `snapshotElapsedRealtimeMs` единственного движка,
 * по которой видимый экран рассчитывает плавное положение кольца на кадрах без создания второго таймера.
 */
data class PhaseEngineSnapshot(
    val runId: String? = null,
    val slotId: String? = null,
    val startDate: String = "2026-11-03",
    val zoneId: String = "UTC",
    val status: RunStatus = RunStatus.IDLE,
    val protocol: DemoProtocolConfig = DEFAULT_DEMO_PROTOCOLS.first(),
    val soundPackSnapshot: SoundPack = SoundPack.SOFT,
    val metronomeStyleSnapshot: MetronomeTickStyle = MetronomeTickStyle.SOFT,
    val currentRepetition: Int = 1,
    val totalRepetitions: Int = protocol.repetitions,
    val currentPhase: PhaseType = PhaseType.CONTRACTION,
    val phaseRemainingMs: Long = protocol.contractionMs,
    val phaseDurationMs: Long = protocol.contractionMs,
    val prepRemainingMs: Long = 0L,
    val confirmedActiveMs: Long = 0L,
    val totalActiveDurationMs: Long = protocol.totalActiveDurationMs,
    val remainingActiveMs: Long = protocol.totalActiveDurationMs,
    val snapshotElapsedRealtimeMs: Long = 0L,
    val isStopDialogVisible: Boolean = false,
    val interruptionReason: String? = null,
    val lastSchedulerDriftMs: Long = 0L,
    val maxSchedulerDriftMs: Long = 0L,
    val isDemo: Boolean = true,
    val isFreeMode: Boolean = false
) {
    val isActiveOrPaused: Boolean
        get() = status == RunStatus.PREPARING ||
            status == RunStatus.RUNNING ||
            status == RunStatus.PAUSED ||
            status == RunStatus.INTERRUPTED

    val isRunningOrPreparing: Boolean
        get() = status == RunStatus.PREPARING || status == RunStatus.RUNNING

    /**
     * На паузе или при прерывании никогда не оставляем команду удержания как актуальную (ТЗ п. 7, v1.4 п. 6).
     */
    val headlineCommandRu: String
        get() = when (status) {
            RunStatus.IDLE -> "Готов к запуску"
            RunStatus.PREPARING -> "Подготовка… Расслабьтесь"
            RunStatus.RUNNING -> currentPhase.labelRu
            RunStatus.PAUSED -> "Пауза — расслабьтесь"
            RunStatus.INTERRUPTED -> "Пауза — расслабьтесь"
            RunStatus.COMPLETED -> "Тренировка завершена"
            RunStatus.STOPPED -> "Остановлено без зачёта"
        }

    /**
     * Рассчитывает точный визуальный остаток текущей фазы (или подготовки) на кадре отрисовки
     * напрямую от монотонных часов движка.
     * На паузе / прерывании возвращает зафиксированный `phaseRemainingMs` без движения.
     */
    fun computeVisualRemainingMs(nowElapsedRealtimeMs: Long): Long {
        return when (status) {
            RunStatus.PREPARING -> {
                val frameDelta = (nowElapsedRealtimeMs - snapshotElapsedRealtimeMs).coerceIn(0L, 2_500L)
                (prepRemainingMs - frameDelta).coerceIn(0L, 3_000L)
            }
            RunStatus.RUNNING -> {
                val frameDelta = (nowElapsedRealtimeMs - snapshotElapsedRealtimeMs).coerceIn(0L, 2_500L)
                (phaseRemainingMs - frameDelta).coerceIn(0L, phaseDurationMs)
            }
            RunStatus.PAUSED,
            RunStatus.INTERRUPTED,
            RunStatus.IDLE -> phaseRemainingMs.coerceIn(0L, phaseDurationMs.coerceAtLeast(1L))
            RunStatus.COMPLETED,
            RunStatus.STOPPED -> 0L
        }
    }

    /**
     * Непрерывная доля заполнения кольца [0f..1f] для плавного отображения без обратного пролёта.
     */
    fun computeVisualRingProgress(nowElapsedRealtimeMs: Long): Float {
        return when (status) {
            RunStatus.IDLE -> 1f
            RunStatus.PREPARING -> {
                val rem = computeVisualRemainingMs(nowElapsedRealtimeMs)
                (rem.toFloat() / 3_000f).coerceIn(0f, 1f)
            }
            RunStatus.RUNNING,
            RunStatus.PAUSED,
            RunStatus.INTERRUPTED -> {
                val total = phaseDurationMs.coerceAtLeast(1L).toFloat()
                val rem = computeVisualRemainingMs(nowElapsedRealtimeMs)
                (rem.toFloat() / total).coerceIn(0f, 1f)
            }
            RunStatus.COMPLETED,
            RunStatus.STOPPED -> 0f
        }
    }

    /**
     * Целое число оставшихся секунд текущей фазы/подготовки (без десятых).
     */
    fun computeDisplayWholeSeconds(nowElapsedRealtimeMs: Long): Long {
        val ms = when (status) {
            RunStatus.PREPARING,
            RunStatus.RUNNING -> computeVisualRemainingMs(nowElapsedRealtimeMs)
            RunStatus.PAUSED,
            RunStatus.INTERRUPTED,
            RunStatus.IDLE -> phaseRemainingMs
            RunStatus.COMPLETED,
            RunStatus.STOPPED -> 0L
        }
        return ((ms + 999L) / 1000L).coerceAtLeast(0L)
    }
}

/**
 * Источник монотонного времени (не зависит от перевода системных часов).
 */
fun interface MonotonicClock {
    fun elapsedRealtimeMs(): Long
}

/**
 * Провайдер времени и часового пояса для управляемого тестирования полуночи, смены зоны и контрольных точек.
 */
interface TimeProvider : MonotonicClock {
    fun currentWallTimeMs(): Long
    fun currentLocalDate(): String
    fun currentZoneId(): String
    fun bootSessionId(): String
}

class SystemTimeProvider : TimeProvider {
    override fun elapsedRealtimeMs(): Long = android.os.SystemClock.elapsedRealtime()
    override fun currentWallTimeMs(): Long = System.currentTimeMillis()
    override fun currentLocalDate(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return sdf.format(java.util.Date(currentWallTimeMs()))
    }
    override fun currentZoneId(): String = java.util.TimeZone.getDefault().id
    override fun bootSessionId(): String {
        val approxBootEpochSec = (currentWallTimeMs() - elapsedRealtimeMs()) / 10_000L
        return "boot_$approxBootEpochSec"
    }
}

/**
 * Причина сохранения контрольной точки в БД Room (ТЗ п. 9).
 */
enum class CheckpointTrigger {
    START,
    PHASE_CHANGE,
    PERIODIC_5S,
    PAUSE,
    INTERRUPT,
    RESUME,
    STOP,
    COMPLETE
}

/**
 * Запись технического лога событий (без личных данных).
 */
data class BenchEventLogEntry(
    val id: Long,
    val elapsedMs: Long,
    val category: String,
    val message: String
)

val DEFAULT_DEMO_PROTOCOLS = listOf(
    DemoProtocolConfig(
        id = "demo_slot_morning_fast",
        titleRu = "Демо · Утро: Быстрые (3 повтора × 4 с / 4 с)",
        contractionMs = 4_000L,
        relaxationMs = 4_000L,
        repetitions = 3,
        isLastInGroup = true,
        group = PlanGroup.MORNING
    ),
    DemoProtocolConfig(
        id = "demo_slot_noon_slow",
        titleRu = "Демо · Обед: Медленные (3 повтора × 5 с / 5 с)",
        contractionMs = 5_000L,
        relaxationMs = 5_000L,
        repetitions = 3,
        isLastInGroup = true,
        group = PlanGroup.NOON
    ),
    DemoProtocolConfig(
        id = "demo_slot_evening_hold",
        titleRu = "Демо · Вечер: Удержание (2 повтора × 6 с / 6 с)",
        contractionMs = 6_000L,
        relaxationMs = 6_000L,
        repetitions = 2,
        isLastInGroup = true,
        group = PlanGroup.EVENING
    )
)
