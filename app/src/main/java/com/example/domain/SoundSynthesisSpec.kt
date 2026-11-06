package com.example.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Параметры отдельной ноты/импульса в локальном синтезаторе сигналов.
 * Поддерживает 2-ю и 3-ю гармоники в зоне максимальной чувствительности слуха (1–3.5 кГц),
 * чтобы сигнал был звонким, резким и отчётливо слышимым на динамике смартфона и поверх YouTube/музыки.
 */
data class ToneNoteSpec(
    val frequencyHz: Double,
    val overtoneRatio: Double = 2.0,
    val overtoneAmplitude: Double = 0.45,
    val thirdHarmonicRatio: Double = 3.0,
    val thirdHarmonicAmplitude: Double = 0.25,
    val durationMs: Int,
    val exponentialDecayRate: Double = 0.8,
    val noteGain: Double = 1.0
)

/**
 * Спецификация звукового паттерна сигнала.
 * `relativeLoudnessScale` задаёт целевую относительную громкость сигнала внутри набора:
 * - Основные команды (`CONTRACTION`, `RELAXATION`, `SET_COMPLETED`, `GROUP_COMPLETED`) выровнены по высокому RMS (`1.0`).
 * - Сигнал подготовки (`PREP_TICK`) чуть сдержаннее (`0.86`), чтобы отсчёт перед стартом был звонким, но не громче рабочей фазы.
 * - Метроном (`METRONOME_TICK`) отчётливый, но всегда тише рабочих команд (`0.48`).
 */
data class SignalPatternSpec(
    val notes: List<ToneNoteSpec>,
    val interNoteGapMs: Int = 12,
    val relativeLoudnessScale: Double = 1.0
) {
    val totalDurationMs: Int
        get() = if (notes.isEmpty()) {
            0
        } else {
            notes.sumOf { it.durationMs } + (notes.size - 1) * interNoteGapMs
        }
}

/**
 * Генератор трёх звонких, резких и высокоэффективных звуковых профилей:
 * - «Импульс Pro» (`CRISP`) — яркий спортивный таймер (1046–1568 Гц + гармоники до 3.5 кГц);
 * - «Кристалл» (`CHIMES`) — звонкий хрустальный перелив с чёткой атакой (1175–1760 Гц);
 * - «Радар» (`SOFT`) — плотный тактический бип-сигнал (880–1318 Гц) без глухих низких частот.
 */
object SoundSynthesisSpec {

    const val SAMPLE_RATE_HZ = 24_000
    const val MIN_SUPPORTED_PHASE_MS = 1_000

    // Повышенный целевой RMS (0.56 вместо прежних 0.34) обеспечивает высокую громкость и пробивную читаемость
    private const val TARGET_COMMAND_RMS = 0.56

    fun resolveSignalPattern(
        pack: SoundPack,
        signal: SignalType,
        metronomeStyle: MetronomeTickStyle = MetronomeTickStyle.CRISP
    ): SignalPatternSpec {
        if (signal == SignalType.METRONOME_TICK) {
            return resolveMetronomePattern(metronomeStyle)
        }

        return when (pack) {
            SoundPack.CRISP -> when (signal) {
                SignalType.PREP_TICK -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(
                            frequencyHz = 1046.50, // C6
                            overtoneRatio = 2.0,
                            overtoneAmplitude = 0.50,
                            thirdHarmonicRatio = 3.0,
                            thirdHarmonicAmplitude = 0.30,
                            durationMs = 55,
                            exponentialDecayRate = 0.6
                        )
                    ),
                    interNoteGapMs = 0,
                    relativeLoudnessScale = 0.86
                )
                SignalType.CONTRACTION -> SignalPatternSpec(
                    // Резкий спортивный двойной импульс вверх (C6 1046.5 Гц -> G6 1568 Гц)
                    notes = listOf(
                        ToneNoteSpec(
                            frequencyHz = 1046.50,
                            overtoneRatio = 2.0,
                            overtoneAmplitude = 0.52,
                            thirdHarmonicRatio = 3.0,
                            thirdHarmonicAmplitude = 0.32,
                            durationMs = 70,
                            exponentialDecayRate = 0.5
                        ),
                        ToneNoteSpec(
                            frequencyHz = 1567.98,
                            overtoneRatio = 2.0,
                            overtoneAmplitude = 0.48,
                            thirdHarmonicRatio = 3.0,
                            thirdHarmonicAmplitude = 0.28,
                            durationMs = 95,
                            exponentialDecayRate = 0.5
                        )
                    ),
                    interNoteGapMs = 12,
                    relativeLoudnessScale = 1.0
                )
                SignalType.RELAXATION -> SignalPatternSpec(
                    // Отчётливый звонкий переход на отдых (E6 1318.5 Гц -> A5 880 Гц)
                    notes = listOf(
                        ToneNoteSpec(
                            frequencyHz = 1318.51,
                            overtoneRatio = 2.0,
                            overtoneAmplitude = 0.50,
                            thirdHarmonicRatio = 3.0,
                            thirdHarmonicAmplitude = 0.30,
                            durationMs = 70,
                            exponentialDecayRate = 0.5
                        ),
                        ToneNoteSpec(
                            frequencyHz = 880.00,
                            overtoneRatio = 2.0,
                            overtoneAmplitude = 0.55,
                            thirdHarmonicRatio = 3.0,
                            thirdHarmonicAmplitude = 0.34,
                            durationMs = 95,
                            exponentialDecayRate = 0.5
                        )
                    ),
                    interNoteGapMs = 12,
                    relativeLoudnessScale = 1.0
                )
                SignalType.PAUSED -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(
                            frequencyHz = 987.77,
                            overtoneRatio = 2.0,
                            overtoneAmplitude = 0.45,
                            thirdHarmonicRatio = 3.0,
                            thirdHarmonicAmplitude = 0.25,
                            durationMs = 65,
                            exponentialDecayRate = 0.6
                        ),
                        ToneNoteSpec(
                            frequencyHz = 783.99,
                            overtoneRatio = 2.0,
                            overtoneAmplitude = 0.48,
                            thirdHarmonicRatio = 3.0,
                            thirdHarmonicAmplitude = 0.28,
                            durationMs = 85,
                            exponentialDecayRate = 0.6
                        )
                    ),
                    interNoteGapMs = 12,
                    relativeLoudnessScale = 0.94
                )
                SignalType.SET_COMPLETED -> SignalPatternSpec(
                    // Яркое спортивное трезвучие финала (C6 -> E6 -> G6)
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 1046.50, overtoneRatio = 2.0, overtoneAmplitude = 0.48, thirdHarmonicRatio = 3.0, thirdHarmonicAmplitude = 0.28, durationMs = 60, exponentialDecayRate = 0.5),
                        ToneNoteSpec(frequencyHz = 1318.51, overtoneRatio = 2.0, overtoneAmplitude = 0.48, thirdHarmonicRatio = 3.0, thirdHarmonicAmplitude = 0.28, durationMs = 60, exponentialDecayRate = 0.5),
                        ToneNoteSpec(frequencyHz = 1567.98, overtoneRatio = 2.0, overtoneAmplitude = 0.48, thirdHarmonicRatio = 3.0, thirdHarmonicAmplitude = 0.28, durationMs = 105, exponentialDecayRate = 0.5)
                    ),
                    interNoteGapMs = 10,
                    relativeLoudnessScale = 1.0
                )
                SignalType.GROUP_COMPLETED -> SignalPatternSpec(
                    // Победная фанфара закрытия этапа (C6 -> E6 -> G6 -> C7)
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 1046.50, overtoneRatio = 2.0, overtoneAmplitude = 0.48, thirdHarmonicRatio = 3.0, thirdHarmonicAmplitude = 0.26, durationMs = 55, exponentialDecayRate = 0.5),
                        ToneNoteSpec(frequencyHz = 1318.51, overtoneRatio = 2.0, overtoneAmplitude = 0.48, thirdHarmonicRatio = 3.0, thirdHarmonicAmplitude = 0.26, durationMs = 55, exponentialDecayRate = 0.5),
                        ToneNoteSpec(frequencyHz = 1567.98, overtoneRatio = 2.0, overtoneAmplitude = 0.48, thirdHarmonicRatio = 3.0, thirdHarmonicAmplitude = 0.26, durationMs = 60, exponentialDecayRate = 0.5),
                        ToneNoteSpec(frequencyHz = 2093.00, overtoneRatio = 2.0, overtoneAmplitude = 0.42, thirdHarmonicRatio = 2.5, thirdHarmonicAmplitude = 0.20, durationMs = 110, exponentialDecayRate = 0.5)
                    ),
                    interNoteGapMs = 10,
                    relativeLoudnessScale = 1.0
                )
                SignalType.METRONOME_TICK -> resolveMetronomePattern(metronomeStyle)
            }

            SoundPack.CHIMES -> when (signal) {
                SignalType.PREP_TICK -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(
                            frequencyHz = 1174.66, // D6
                            overtoneRatio = 2.4,
                            overtoneAmplitude = 0.46,
                            thirdHarmonicRatio = 3.2,
                            thirdHarmonicAmplitude = 0.24,
                            durationMs = 55,
                            exponentialDecayRate = 1.1
                        )
                    ),
                    interNoteGapMs = 0,
                    relativeLoudnessScale = 0.86
                )
                SignalType.CONTRACTION -> SignalPatternSpec(
                    // Звонкий хрустальный перелив вверх (D6 1174.66 Гц -> A6 1760 Гц)
                    notes = listOf(
                        ToneNoteSpec(
                            frequencyHz = 1174.66,
                            overtoneRatio = 2.4,
                            overtoneAmplitude = 0.48,
                            thirdHarmonicRatio = 3.2,
                            thirdHarmonicAmplitude = 0.26,
                            durationMs = 72,
                            exponentialDecayRate = 1.0
                        ),
                        ToneNoteSpec(
                            frequencyHz = 1760.00,
                            overtoneRatio = 2.4,
                            overtoneAmplitude = 0.44,
                            thirdHarmonicRatio = 3.0,
                            thirdHarmonicAmplitude = 0.22,
                            durationMs = 95,
                            exponentialDecayRate = 1.0
                        )
                    ),
                    interNoteGapMs = 12,
                    relativeLoudnessScale = 1.0
                )
                SignalType.RELAXATION -> SignalPatternSpec(
                    // Звонкий хрустальный перелив вниз (F#6 1479.98 Гц -> A5 880 Гц)
                    notes = listOf(
                        ToneNoteSpec(
                            frequencyHz = 1479.98,
                            overtoneRatio = 2.4,
                            overtoneAmplitude = 0.46,
                            thirdHarmonicRatio = 3.0,
                            thirdHarmonicAmplitude = 0.24,
                            durationMs = 72,
                            exponentialDecayRate = 1.0
                        ),
                        ToneNoteSpec(
                            frequencyHz = 880.00,
                            overtoneRatio = 2.4,
                            overtoneAmplitude = 0.50,
                            thirdHarmonicRatio = 3.2,
                            thirdHarmonicAmplitude = 0.28,
                            durationMs = 95,
                            exponentialDecayRate = 1.0
                        )
                    ),
                    interNoteGapMs = 12,
                    relativeLoudnessScale = 1.0
                )
                SignalType.PAUSED -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(
                            frequencyHz = 1046.50,
                            overtoneRatio = 2.4,
                            overtoneAmplitude = 0.42,
                            thirdHarmonicRatio = 3.0,
                            thirdHarmonicAmplitude = 0.22,
                            durationMs = 65,
                            exponentialDecayRate = 1.1
                        ),
                        ToneNoteSpec(
                            frequencyHz = 783.99,
                            overtoneRatio = 2.4,
                            overtoneAmplitude = 0.45,
                            thirdHarmonicRatio = 3.0,
                            thirdHarmonicAmplitude = 0.24,
                            durationMs = 85,
                            exponentialDecayRate = 1.1
                        )
                    ),
                    interNoteGapMs = 12,
                    relativeLoudnessScale = 0.94
                )
                SignalType.SET_COMPLETED -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 987.77, overtoneRatio = 2.4, overtoneAmplitude = 0.45, thirdHarmonicRatio = 3.0, thirdHarmonicAmplitude = 0.24, durationMs = 65, exponentialDecayRate = 1.0),
                        ToneNoteSpec(frequencyHz = 1174.66, overtoneRatio = 2.4, overtoneAmplitude = 0.45, thirdHarmonicRatio = 3.0, thirdHarmonicAmplitude = 0.24, durationMs = 65, exponentialDecayRate = 1.0),
                        ToneNoteSpec(frequencyHz = 1567.98, overtoneRatio = 2.4, overtoneAmplitude = 0.45, thirdHarmonicRatio = 3.0, thirdHarmonicAmplitude = 0.24, durationMs = 105, exponentialDecayRate = 1.0)
                    ),
                    interNoteGapMs = 10,
                    relativeLoudnessScale = 1.0
                )
                SignalType.GROUP_COMPLETED -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 880.00, overtoneRatio = 2.4, overtoneAmplitude = 0.45, thirdHarmonicRatio = 3.0, thirdHarmonicAmplitude = 0.24, durationMs = 58, exponentialDecayRate = 1.0),
                        ToneNoteSpec(frequencyHz = 1174.66, overtoneRatio = 2.4, overtoneAmplitude = 0.45, thirdHarmonicRatio = 3.0, thirdHarmonicAmplitude = 0.24, durationMs = 58, exponentialDecayRate = 1.0),
                        ToneNoteSpec(frequencyHz = 1479.98, overtoneRatio = 2.4, overtoneAmplitude = 0.45, thirdHarmonicRatio = 3.0, thirdHarmonicAmplitude = 0.24, durationMs = 62, exponentialDecayRate = 1.0),
                        ToneNoteSpec(frequencyHz = 1760.00, overtoneRatio = 2.4, overtoneAmplitude = 0.45, thirdHarmonicRatio = 3.0, thirdHarmonicAmplitude = 0.24, durationMs = 110, exponentialDecayRate = 1.0)
                    ),
                    interNoteGapMs = 10,
                    relativeLoudnessScale = 1.0
                )
                SignalType.METRONOME_TICK -> resolveMetronomePattern(metronomeStyle)
            }

            SoundPack.SOFT -> when (signal) {
                SignalType.PREP_TICK -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(
                            frequencyHz = 880.00, // A5 — плотный тактический тон
                            overtoneRatio = 2.0,
                            overtoneAmplitude = 0.48,
                            thirdHarmonicRatio = 3.0,
                            thirdHarmonicAmplitude = 0.26,
                            durationMs = 58,
                            exponentialDecayRate = 0.5
                        )
                    ),
                    interNoteGapMs = 0,
                    relativeLoudnessScale = 0.86
                )
                SignalType.CONTRACTION -> SignalPatternSpec(
                    // Тактический сфокусированный подъём (A5 880 Гц -> E6 1318.51 Гц)
                    notes = listOf(
                        ToneNoteSpec(
                            frequencyHz = 880.00,
                            overtoneRatio = 2.0,
                            overtoneAmplitude = 0.50,
                            thirdHarmonicRatio = 3.0,
                            thirdHarmonicAmplitude = 0.28,
                            durationMs = 75,
                            exponentialDecayRate = 0.5
                        ),
                        ToneNoteSpec(
                            frequencyHz = 1318.51,
                            overtoneRatio = 2.0,
                            overtoneAmplitude = 0.48,
                            thirdHarmonicRatio = 3.0,
                            thirdHarmonicAmplitude = 0.26,
                            durationMs = 95,
                            exponentialDecayRate = 0.5
                        )
                    ),
                    interNoteGapMs = 12,
                    relativeLoudnessScale = 1.0
                )
                SignalType.RELAXATION -> SignalPatternSpec(
                    // Тактический сфокусированный спад (C6 1046.50 Гц -> G5 783.99 Гц)
                    notes = listOf(
                        ToneNoteSpec(
                            frequencyHz = 1046.50,
                            overtoneRatio = 2.0,
                            overtoneAmplitude = 0.48,
                            thirdHarmonicRatio = 3.0,
                            thirdHarmonicAmplitude = 0.28,
                            durationMs = 75,
                            exponentialDecayRate = 0.5
                        ),
                        ToneNoteSpec(
                            frequencyHz = 783.99,
                            overtoneRatio = 2.0,
                            overtoneAmplitude = 0.54,
                            thirdHarmonicRatio = 3.0,
                            thirdHarmonicAmplitude = 0.32,
                            durationMs = 95,
                            exponentialDecayRate = 0.5
                        )
                    ),
                    interNoteGapMs = 12,
                    relativeLoudnessScale = 1.0
                )
                SignalType.PAUSED -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(
                            frequencyHz = 880.00,
                            overtoneRatio = 2.0,
                            overtoneAmplitude = 0.45,
                            thirdHarmonicRatio = 3.0,
                            thirdHarmonicAmplitude = 0.25,
                            durationMs = 65,
                            exponentialDecayRate = 0.5
                        ),
                        ToneNoteSpec(
                            frequencyHz = 659.25,
                            overtoneRatio = 2.0,
                            overtoneAmplitude = 0.50,
                            thirdHarmonicRatio = 3.0,
                            thirdHarmonicAmplitude = 0.28,
                            durationMs = 85,
                            exponentialDecayRate = 0.5
                        )
                    ),
                    interNoteGapMs = 12,
                    relativeLoudnessScale = 0.94
                )
                SignalType.SET_COMPLETED -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 783.99, overtoneRatio = 2.0, overtoneAmplitude = 0.48, thirdHarmonicRatio = 3.0, thirdHarmonicAmplitude = 0.26, durationMs = 65, exponentialDecayRate = 0.5),
                        ToneNoteSpec(frequencyHz = 987.77, overtoneRatio = 2.0, overtoneAmplitude = 0.48, thirdHarmonicRatio = 3.0, thirdHarmonicAmplitude = 0.26, durationMs = 65, exponentialDecayRate = 0.5),
                        ToneNoteSpec(frequencyHz = 1318.51, overtoneRatio = 2.0, overtoneAmplitude = 0.48, thirdHarmonicRatio = 3.0, thirdHarmonicAmplitude = 0.26, durationMs = 105, exponentialDecayRate = 0.5)
                    ),
                    interNoteGapMs = 10,
                    relativeLoudnessScale = 1.0
                )
                SignalType.GROUP_COMPLETED -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 783.99, overtoneRatio = 2.0, overtoneAmplitude = 0.48, thirdHarmonicRatio = 3.0, thirdHarmonicAmplitude = 0.26, durationMs = 60, exponentialDecayRate = 0.5),
                        ToneNoteSpec(frequencyHz = 987.77, overtoneRatio = 2.0, overtoneAmplitude = 0.48, thirdHarmonicRatio = 3.0, thirdHarmonicAmplitude = 0.26, durationMs = 60, exponentialDecayRate = 0.5),
                        ToneNoteSpec(frequencyHz = 1174.66, overtoneRatio = 2.0, overtoneAmplitude = 0.48, thirdHarmonicRatio = 3.0, thirdHarmonicAmplitude = 0.26, durationMs = 65, exponentialDecayRate = 0.5),
                        ToneNoteSpec(frequencyHz = 1567.98, overtoneRatio = 2.0, overtoneAmplitude = 0.48, thirdHarmonicRatio = 3.0, thirdHarmonicAmplitude = 0.26, durationMs = 110, exponentialDecayRate = 0.5)
                    ),
                    interNoteGapMs = 10,
                    relativeLoudnessScale = 1.0
                )
                SignalType.METRONOME_TICK -> resolveMetronomePattern(metronomeStyle)
            }
        }
    }

    fun resolveMetronomePattern(style: MetronomeTickStyle): SignalPatternSpec {
        return when (style) {
            MetronomeTickStyle.SOFT -> SignalPatternSpec(
                notes = listOf(
                    ToneNoteSpec(
                        frequencyHz = 1200.0,
                        overtoneRatio = 2.0,
                        overtoneAmplitude = 0.42,
                        thirdHarmonicRatio = 3.0,
                        thirdHarmonicAmplitude = 0.20,
                        durationMs = 26,
                        exponentialDecayRate = 2.0
                    )
                ),
                interNoteGapMs = 0,
                relativeLoudnessScale = 0.48
            )
            MetronomeTickStyle.CRISP -> SignalPatternSpec(
                notes = listOf(
                    ToneNoteSpec(
                        frequencyHz = 1680.0,
                        overtoneRatio = 2.0,
                        overtoneAmplitude = 0.48,
                        thirdHarmonicRatio = 2.5,
                        thirdHarmonicAmplitude = 0.26,
                        durationMs = 24,
                        exponentialDecayRate = 2.2
                    )
                ),
                interNoteGapMs = 0,
                relativeLoudnessScale = 0.48
            )
        }
    }

    /**
     * Вычисляет эффективный уровень громкости [0f..1f] с гарантией, что тик метронома
     * всегда тише рабочих команд, а образцы и сигналы сессии используют единую формулу.
     */
    fun computeEffectiveVolume(
        signal: SignalType,
        signalVolume: Float,
        metronomeVolume: Float
    ): Float {
        val clampedSignal = signalVolume.coerceIn(0f, 1f)
        val clampedMetro = metronomeVolume.coerceIn(0f, 1f)
        return if (signal == SignalType.METRONOME_TICK) {
            val maxAllowedMetro = if (clampedSignal > 0.05f) {
                (clampedSignal * 0.68f).coerceAtLeast(0.10f)
            } else {
                clampedMetro * 0.58f
            }
            clampedMetro.coerceAtMost(maxAllowedMetro)
        } else {
            clampedSignal
        }
    }

    /**
     * Вычисляет нормализованный среднеквадратичный уровень (RMS) ненулевых отсчётов буфера в диапазоне [0f..1f].
     */
    fun computeActiveRms(pcm: ShortArray): Float {
        if (pcm.isEmpty()) return 0f
        var sumSq = 0.0
        var activeCount = 0
        for (sample in pcm) {
            if (sample.toInt() != 0) {
                val norm = sample.toDouble() / Short.MAX_VALUE.toDouble()
                sumSq += norm * norm
                activeCount++
            }
        }
        if (activeCount == 0) return 0f
        return sqrt(sumSq / activeCount.toDouble()).toFloat()
    }

    /**
     * Генерирует 16-битный моно PCM-буфер с быстрой чёткой атакой (4 мс) и мягким сглаженным спадом (10 мс),
     * исключающими щелчки в начале и конце каждой ноты, и выравнивает среднеквадратичную громкость (RMS)
     * на высоком пробивном уровне (TARGET_COMMAND_RMS = 0.56).
     */
    fun synthesizePcm16(
        pattern: SignalPatternSpec,
        volume: Float,
        sampleRate: Int = SAMPLE_RATE_HZ
    ): ShortArray {
        val clampedVol = volume.coerceIn(0f, 1f)
        if (clampedVol <= 0.005f || pattern.notes.isEmpty()) {
            return ShortArray(0)
        }

        val totalMs = pattern.totalDurationMs
        val totalSamples = (sampleRate * totalMs) / 1000
        if (totalSamples <= 0) return ShortArray(0)

        val rawFloat = DoubleArray(totalSamples)
        var writeIdx = 0
        var activeSamplesSumSq = 0.0
        var activeSamplesCount = 0

        for ((noteIdx, note) in pattern.notes.withIndex()) {
            val noteSamples = (sampleRate * note.durationMs) / 1000
            if (noteSamples <= 0) continue

            // Быстрая звонкая атака (4 мс) и чистый спад (10 мс)
            val attackSamples = ((sampleRate * 4) / 1000).coerceIn(1, (noteSamples / 4).coerceAtLeast(1))
            val releaseSamples = ((sampleRate * 10) / 1000).coerceIn(1, (noteSamples / 3).coerceAtLeast(1))
            val normFactor = 1.0 + note.overtoneAmplitude + note.thirdHarmonicAmplitude

            for (s in 0 until noteSamples) {
                if (writeIdx >= rawFloat.size) break

                val env = when {
                    s == 0 || s == noteSamples - 1 -> 0.0
                    s < attackSamples -> 0.5 * (1.0 - cos(PI * s.toDouble() / attackSamples))
                    s >= noteSamples - releaseSamples -> {
                        val relPos = (noteSamples - 1 - s).toDouble() / releaseSamples
                        0.5 * (1.0 - cos(PI * relPos))
                    }
                    else -> 1.0
                }

                val progress = s.toDouble() / noteSamples.toDouble()
                val decay = if (note.exponentialDecayRate > 0.0) {
                    exp(-note.exponentialDecayRate * progress)
                } else {
                    1.0
                }

                val t = s.toDouble() / sampleRate.toDouble()
                var wave = sin(2.0 * PI * note.frequencyHz * t)
                if (note.overtoneAmplitude > 0.0 && note.overtoneRatio > 0.0) {
                    wave += note.overtoneAmplitude * sin(2.0 * PI * note.frequencyHz * note.overtoneRatio * t)
                }
                if (note.thirdHarmonicAmplitude > 0.0 && note.thirdHarmonicRatio > 0.0) {
                    wave += note.thirdHarmonicAmplitude * sin(2.0 * PI * note.frequencyHz * note.thirdHarmonicRatio * t)
                }
                wave = (wave / normFactor) * env * decay * note.noteGain

                rawFloat[writeIdx++] = wave
                if (s in 1 until (noteSamples - 1)) {
                    activeSamplesSumSq += wave * wave
                    activeSamplesCount++
                }
            }

            if (writeIdx > 0 && writeIdx <= rawFloat.size) {
                rawFloat[writeIdx - 1] = 0.0
            }

            if (noteIdx < pattern.notes.lastIndex && pattern.interNoteGapMs > 0) {
                val gapSamples = (sampleRate * pattern.interNoteGapMs) / 1000
                writeIdx = (writeIdx + gapSamples).coerceAtMost(rawFloat.size)
            }
        }

        // Выравнивание RMS по высокому целевому уровню без клиппинга
        val rawRms = if (activeSamplesCount > 0) {
            sqrt(activeSamplesSumSq / activeSamplesCount.toDouble()).coerceAtLeast(0.05)
        } else {
            0.5
        }
        val targetRms = TARGET_COMMAND_RMS * pattern.relativeLoudnessScale * clampedVol.toDouble()
        val rmsGain = (targetRms / rawRms).coerceAtMost(1.45 * clampedVol.toDouble())

        val pcm = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val scaled = (rawFloat[i] * rmsGain).coerceIn(-0.98, 0.98)
            pcm[i] = (scaled * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }

        // Гарантируем строгий 0 на границах всего буфера и каждой ноты
        var cursor = 0
        for ((noteIdx, note) in pattern.notes.withIndex()) {
            val noteSamples = (sampleRate * note.durationMs) / 1000
            if (cursor in pcm.indices) pcm[cursor] = 0
            val endNote = (cursor + noteSamples - 1).coerceIn(0, pcm.lastIndex)
            pcm[endNote] = 0
            cursor += noteSamples
            if (noteIdx < pattern.notes.lastIndex && pattern.interNoteGapMs > 0) {
                cursor += (sampleRate * pattern.interNoteGapMs) / 1000
            }
        }
        if (pcm.isNotEmpty()) {
            pcm[0] = 0
            pcm[pcm.lastIndex] = 0
        }
        return pcm
    }
}
