package com.example.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Параметры отдельной ноты/импульса в локальном синтезаторе сигналов.
 */
data class ToneNoteSpec(
    val frequencyHz: Double,
    val overtoneRatio: Double = 0.0,
    val overtoneAmplitude: Double = 0.0,
    val durationMs: Int,
    val exponentialDecayRate: Double = 0.0
)

/**
 * Спецификация звукового паттерна сигнала.
 */
data class SignalPatternSpec(
    val notes: List<ToneNoteSpec>,
    val interNoteGapMs: Int = 14
) {
    val totalDurationMs: Int
        get() = if (notes.isEmpty()) {
            0
        } else {
            notes.sumOf { it.durationMs } + (notes.size - 1) * interNoteGapMs
        }
}

/**
 * Чистый Kotlin-генератор локальных звуковых наборов («Мягкий», «Колокольчики», «Чёткий»)
 * и двух вариантов тика метронома («Мягкий», «Чёткий»).
 *
 * Гарантирует:
 * - различимость команд «Сокращение», «Расслабление», «Подход завершён», «Группа завершена» внутри каждого набора;
 * - отсутствие щелчков на начале/конце за счёт сглаженного косинусного окна (raised-cosine attack/release) и старта/финиша в нуле;
 * - короткую длительность (<= 360 мс, для команд фаз <= 220 мс) без длинного хвоста, пригодную для самой короткой фазы (1000 мс).
 */
object SoundSynthesisSpec {

    const val SAMPLE_RATE_HZ = 24_000
    const val MIN_SUPPORTED_PHASE_MS = 1_000

    fun resolveSignalPattern(
        pack: SoundPack,
        signal: SignalType,
        metronomeStyle: MetronomeTickStyle = MetronomeTickStyle.SOFT
    ): SignalPatternSpec {
        if (signal == SignalType.METRONOME_TICK) {
            return resolveMetronomePattern(metronomeStyle)
        }

        return when (pack) {
            SoundPack.SOFT -> when (signal) {
                SignalType.PREP_TICK -> SignalPatternSpec(
                    notes = listOf(ToneNoteSpec(frequencyHz = 440.0, durationMs = 55)),
                    interNoteGapMs = 0
                )
                SignalType.CONTRACTION -> SignalPatternSpec(
                    // Спокойный мягкий подъём (Ре4 -> Ля4)
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 293.66, overtoneRatio = 2.0, overtoneAmplitude = 0.12, durationMs = 75),
                        ToneNoteSpec(frequencyHz = 440.00, overtoneRatio = 2.0, overtoneAmplitude = 0.12, durationMs = 95)
                    ),
                    interNoteGapMs = 14
                )
                SignalType.RELAXATION -> SignalPatternSpec(
                    // Спокойный мягкий спад (Ми4 -> До4)
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 329.63, overtoneRatio = 2.0, overtoneAmplitude = 0.10, durationMs = 80),
                        ToneNoteSpec(frequencyHz = 261.63, overtoneRatio = 2.0, overtoneAmplitude = 0.10, durationMs = 105)
                    ),
                    interNoteGapMs = 14
                )
                SignalType.PAUSED -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 349.23, durationMs = 65),
                        ToneNoteSpec(frequencyHz = 261.63, durationMs = 85)
                    ),
                    interNoteGapMs = 14
                )
                SignalType.SET_COMPLETED -> SignalPatternSpec(
                    // Тёплое трезвучие завершения подхода
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 261.63, durationMs = 70),
                        ToneNoteSpec(frequencyHz = 329.63, durationMs = 70),
                        ToneNoteSpec(frequencyHz = 392.00, durationMs = 115)
                    ),
                    interNoteGapMs = 12
                )
                SignalType.GROUP_COMPLETED -> SignalPatternSpec(
                    // Четырёхнотный мягкий финал группы
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 261.63, durationMs = 65),
                        ToneNoteSpec(frequencyHz = 329.63, durationMs = 65),
                        ToneNoteSpec(frequencyHz = 392.00, durationMs = 70),
                        ToneNoteSpec(frequencyHz = 523.25, durationMs = 125)
                    ),
                    interNoteGapMs = 10
                )
                SignalType.METRONOME_TICK -> resolveMetronomePattern(metronomeStyle)
            }

            SoundPack.CHIMES -> when (signal) {
                SignalType.PREP_TICK -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(
                            frequencyHz = 783.99,
                            overtoneRatio = 2.76,
                            overtoneAmplitude = 0.25,
                            durationMs = 50,
                            exponentialDecayRate = 4.0
                        )
                    ),
                    interNoteGapMs = 0
                )
                SignalType.CONTRACTION -> SignalPatternSpec(
                    // Чистый светлый перелив вверх (Соль5 -> Ре6)
                    notes = listOf(
                        ToneNoteSpec(
                            frequencyHz = 783.99,
                            overtoneRatio = 2.76,
                            overtoneAmplitude = 0.28,
                            durationMs = 70,
                            exponentialDecayRate = 4.2
                        ),
                        ToneNoteSpec(
                            frequencyHz = 1174.66,
                            overtoneRatio = 2.76,
                            overtoneAmplitude = 0.24,
                            durationMs = 95,
                            exponentialDecayRate = 4.5
                        )
                    ),
                    interNoteGapMs = 12
                )
                SignalType.RELAXATION -> SignalPatternSpec(
                    // Чистый перелив вниз (Ля5 -> Ре5)
                    notes = listOf(
                        ToneNoteSpec(
                            frequencyHz = 880.00,
                            overtoneRatio = 2.76,
                            overtoneAmplitude = 0.24,
                            durationMs = 75,
                            exponentialDecayRate = 4.2
                        ),
                        ToneNoteSpec(
                            frequencyHz = 587.33,
                            overtoneRatio = 2.76,
                            overtoneAmplitude = 0.22,
                            durationMs = 100,
                            exponentialDecayRate = 4.5
                        )
                    ),
                    interNoteGapMs = 12
                )
                SignalType.PAUSED -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(
                            frequencyHz = 659.25,
                            overtoneRatio = 2.5,
                            overtoneAmplitude = 0.20,
                            durationMs = 60,
                            exponentialDecayRate = 4.5
                        ),
                        ToneNoteSpec(
                            frequencyHz = 523.25,
                            overtoneRatio = 2.5,
                            overtoneAmplitude = 0.20,
                            durationMs = 80,
                            exponentialDecayRate = 4.5
                        )
                    ),
                    interNoteGapMs = 12
                )
                SignalType.SET_COMPLETED -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 659.25, overtoneRatio = 2.76, overtoneAmplitude = 0.25, durationMs = 65, exponentialDecayRate = 4.0),
                        ToneNoteSpec(frequencyHz = 783.99, overtoneRatio = 2.76, overtoneAmplitude = 0.25, durationMs = 65, exponentialDecayRate = 4.0),
                        ToneNoteSpec(frequencyHz = 1046.50, overtoneRatio = 2.76, overtoneAmplitude = 0.25, durationMs = 110, exponentialDecayRate = 4.5)
                    ),
                    interNoteGapMs = 10
                )
                SignalType.GROUP_COMPLETED -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 587.33, overtoneRatio = 2.76, overtoneAmplitude = 0.25, durationMs = 60, exponentialDecayRate = 4.0),
                        ToneNoteSpec(frequencyHz = 783.99, overtoneRatio = 2.76, overtoneAmplitude = 0.25, durationMs = 60, exponentialDecayRate = 4.0),
                        ToneNoteSpec(frequencyHz = 987.77, overtoneRatio = 2.76, overtoneAmplitude = 0.25, durationMs = 65, exponentialDecayRate = 4.0),
                        ToneNoteSpec(frequencyHz = 1318.51, overtoneRatio = 2.76, overtoneAmplitude = 0.25, durationMs = 115, exponentialDecayRate = 4.8)
                    ),
                    interNoteGapMs = 10
                )
                SignalType.METRONOME_TICK -> resolveMetronomePattern(metronomeStyle)
            }

            SoundPack.CRISP -> when (signal) {
                SignalType.PREP_TICK -> SignalPatternSpec(
                    notes = listOf(ToneNoteSpec(frequencyHz = 880.0, overtoneRatio = 3.0, overtoneAmplitude = 0.18, durationMs = 45)),
                    interNoteGapMs = 0
                )
                SignalType.CONTRACTION -> SignalPatternSpec(
                    // Отчётливый короткий дуплет вверх (740 Гц -> 1108 Гц)
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 739.99, overtoneRatio = 3.0, overtoneAmplitude = 0.20, durationMs = 60),
                        ToneNoteSpec(frequencyHz = 1108.73, overtoneRatio = 3.0, overtoneAmplitude = 0.20, durationMs = 80)
                    ),
                    interNoteGapMs = 12
                )
                SignalType.RELAXATION -> SignalPatternSpec(
                    // Отчётливый короткий дуплет вниз (830 Гц -> 554 Гц)
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 830.61, overtoneRatio = 3.0, overtoneAmplitude = 0.18, durationMs = 65),
                        ToneNoteSpec(frequencyHz = 554.37, overtoneRatio = 3.0, overtoneAmplitude = 0.18, durationMs = 85)
                    ),
                    interNoteGapMs = 12
                )
                SignalType.PAUSED -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 622.25, overtoneRatio = 3.0, overtoneAmplitude = 0.15, durationMs = 55),
                        ToneNoteSpec(frequencyHz = 466.16, overtoneRatio = 3.0, overtoneAmplitude = 0.15, durationMs = 75)
                    ),
                    interNoteGapMs = 12
                )
                SignalType.SET_COMPLETED -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 659.25, overtoneRatio = 3.0, overtoneAmplitude = 0.18, durationMs = 55),
                        ToneNoteSpec(frequencyHz = 830.61, overtoneRatio = 3.0, overtoneAmplitude = 0.18, durationMs = 55),
                        ToneNoteSpec(frequencyHz = 987.77, overtoneRatio = 3.0, overtoneAmplitude = 0.18, durationMs = 95)
                    ),
                    interNoteGapMs = 10
                )
                SignalType.GROUP_COMPLETED -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 659.25, overtoneRatio = 3.0, overtoneAmplitude = 0.18, durationMs = 50),
                        ToneNoteSpec(frequencyHz = 830.61, overtoneRatio = 3.0, overtoneAmplitude = 0.18, durationMs = 50),
                        ToneNoteSpec(frequencyHz = 987.77, overtoneRatio = 3.0, overtoneAmplitude = 0.18, durationMs = 55),
                        ToneNoteSpec(frequencyHz = 1318.51, overtoneRatio = 3.0, overtoneAmplitude = 0.18, durationMs = 105)
                    ),
                    interNoteGapMs = 10
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
                        frequencyHz = 420.0,
                        overtoneRatio = 0.0,
                        overtoneAmplitude = 0.0,
                        durationMs = 28,
                        exponentialDecayRate = 5.5
                    )
                ),
                interNoteGapMs = 0
            )
            MetronomeTickStyle.CRISP -> SignalPatternSpec(
                notes = listOf(
                    ToneNoteSpec(
                        frequencyHz = 920.0,
                        overtoneRatio = 2.0,
                        overtoneAmplitude = 0.22,
                        durationMs = 22,
                        exponentialDecayRate = 6.0
                    )
                ),
                interNoteGapMs = 0
            )
        }
    }

    /**
     * Генерирует 16-битный моно PCM-буфер со сглаженными косинусными фронтами (raised-cosine window),
     * исключающими щелчки в начале и конце каждой ноты.
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

        val pcm = ShortArray(totalSamples)
        var writeIdx = 0

        for ((noteIdx, note) in pattern.notes.withIndex()) {
            val noteSamples = (sampleRate * note.durationMs) / 1000
            if (noteSamples <= 0) continue

            // Плавный косинусный вход (8 мс) и выход (12 мс) без скачка амплитуды
            val attackSamples = ((sampleRate * 8) / 1000).coerceIn(1, (noteSamples / 3).coerceAtLeast(1))
            val releaseSamples = ((sampleRate * 12) / 1000).coerceIn(1, (noteSamples / 2).coerceAtLeast(1))
            val normFactor = 1.0 + note.overtoneAmplitude

            for (s in 0 until noteSamples) {
                if (writeIdx >= pcm.size) break

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
                wave /= normFactor

                val sampleDouble = wave * env * decay * clampedVol * Short.MAX_VALUE * 0.78
                pcm[writeIdx++] = sampleDouble.toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }

            // Принудительно гарантируем ноль на границах ноты
            if (writeIdx > 0 && writeIdx <= pcm.size) {
                pcm[writeIdx - 1] = 0
            }

            if (noteIdx < pattern.notes.lastIndex && pattern.interNoteGapMs > 0) {
                val gapSamples = (sampleRate * pattern.interNoteGapMs) / 1000
                writeIdx = (writeIdx + gapSamples).coerceAtMost(pcm.size)
            }
        }

        if (pcm.isNotEmpty()) {
            pcm[0] = 0
            pcm[pcm.lastIndex] = 0
        }
        return pcm
    }
}
