package com.example.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Параметры отдельной ноты/импульса в локальном синтезаторе сигналов.
 */
data class ToneNoteSpec(
    val frequencyHz: Double,
    val overtoneRatio: Double = 0.0,
    val overtoneAmplitude: Double = 0.0,
    val durationMs: Int,
    val exponentialDecayRate: Double = 0.0,
    val noteGain: Double = 1.0
)

/**
 * Спецификация звукового паттерна сигнала.
 * `relativeLoudnessScale` задаёт целевую относительную громкость сигнала внутри набора:
 * - Основные команды (`CONTRACTION`, `RELAXATION`, `SET_COMPLETED`, `GROUP_COMPLETED`) выровнены по RMS (`1.0`).
 * - Сигнал подготовки (`PREP_TICK`) чуть спокойнее (`0.82`), чтобы старт сессии никогда не звучал резче рабочих команд.
 * - Метроном (`METRONOME_TICK`) всегда заметно тише рабочих команд (`0.42`).
 */
data class SignalPatternSpec(
    val notes: List<ToneNoteSpec>,
    val interNoteGapMs: Int = 14,
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
 * Чистый Kotlin-генератор локальных звуковых наборов («Мягкий», «Колокольчики», «Чёткий»)
 * и двух вариантов тика метронома («Мягкий», «Чёткий»).
 *
 * Гарантирует:
 * - различимость команд «Сокращение», «Расслабление», «Подход завершён», «Группа завершена» внутри каждого набора;
 * - выровненную воспринимаемую громкость (RMS) всех основных команд внутри каждого набора и на динамике телефона
 *   (частоты от 392 Гц с поддержкой 2-й гармоники на нижних нотах, чтобы микродинамик не «проваливал» команду «Расслабление» после старта);
 * - отсутствие щелчков на начале/конце за счёт сглаженного косинусного окна (raised-cosine attack/release) и старта/финиша в нуле;
 * - короткую длительность (<= 360 мс, для команд фаз <= 220 мс) без длинного хвоста, пригодную для самой короткой фазы (1000 мс);
 * - гарантированно более тихий метроном по сравнению с командами фаз.
 */
object SoundSynthesisSpec {

    const val SAMPLE_RATE_HZ = 24_000
    const val MIN_SUPPORTED_PHASE_MS = 1_000

    private const val TARGET_COMMAND_RMS = 0.34

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
                    notes = listOf(
                        ToneNoteSpec(
                            frequencyHz = 440.0,
                            overtoneRatio = 2.0,
                            overtoneAmplitude = 0.20,
                            durationMs = 60
                        )
                    ),
                    interNoteGapMs = 0,
                    relativeLoudnessScale = 0.82
                )
                SignalType.CONTRACTION -> SignalPatternSpec(
                    // Спокойный тёплый подъём (Соль4 392 Гц -> Си4 493.88 Гц) с богатой 2-й гармоникой
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 392.00, overtoneRatio = 2.0, overtoneAmplitude = 0.32, durationMs = 80),
                        ToneNoteSpec(frequencyHz = 493.88, overtoneRatio = 2.0, overtoneAmplitude = 0.28, durationMs = 100)
                    ),
                    interNoteGapMs = 14,
                    relativeLoudnessScale = 1.0
                )
                SignalType.RELAXATION -> SignalPatternSpec(
                    // Спокойный тёплый спад (Ля4 440 Гц -> Фа#4 369.99 Гц с усиленной 2-й гармоникой для динамика)
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 440.00, overtoneRatio = 2.0, overtoneAmplitude = 0.30, durationMs = 80),
                        ToneNoteSpec(frequencyHz = 369.99, overtoneRatio = 2.0, overtoneAmplitude = 0.42, durationMs = 105)
                    ),
                    interNoteGapMs = 14,
                    relativeLoudnessScale = 1.0
                )
                SignalType.PAUSED -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 440.00, overtoneRatio = 2.0, overtoneAmplitude = 0.25, durationMs = 65),
                        ToneNoteSpec(frequencyHz = 349.23, overtoneRatio = 2.0, overtoneAmplitude = 0.38, durationMs = 85)
                    ),
                    interNoteGapMs = 14,
                    relativeLoudnessScale = 0.92
                )
                SignalType.SET_COMPLETED -> SignalPatternSpec(
                    // Тёплое трезвучие завершения подхода (392 -> 493.88 -> 587.33 Гц)
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 392.00, overtoneRatio = 2.0, overtoneAmplitude = 0.30, durationMs = 70),
                        ToneNoteSpec(frequencyHz = 493.88, overtoneRatio = 2.0, overtoneAmplitude = 0.26, durationMs = 70),
                        ToneNoteSpec(frequencyHz = 587.33, overtoneRatio = 2.0, overtoneAmplitude = 0.24, durationMs = 110)
                    ),
                    interNoteGapMs = 12,
                    relativeLoudnessScale = 1.0
                )
                SignalType.GROUP_COMPLETED -> SignalPatternSpec(
                    // Четырёхнотный мягкий финал группы
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 392.00, overtoneRatio = 2.0, overtoneAmplitude = 0.30, durationMs = 65),
                        ToneNoteSpec(frequencyHz = 493.88, overtoneRatio = 2.0, overtoneAmplitude = 0.26, durationMs = 65),
                        ToneNoteSpec(frequencyHz = 587.33, overtoneRatio = 2.0, overtoneAmplitude = 0.24, durationMs = 70),
                        ToneNoteSpec(frequencyHz = 783.99, overtoneRatio = 2.0, overtoneAmplitude = 0.20, durationMs = 120)
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
                            frequencyHz = 783.99,
                            overtoneRatio = 2.76,
                            overtoneAmplitude = 0.25,
                            durationMs = 55,
                            exponentialDecayRate = 1.8
                        )
                    ),
                    interNoteGapMs = 0,
                    relativeLoudnessScale = 0.82
                )
                SignalType.CONTRACTION -> SignalPatternSpec(
                    // Чистый светлый перелив вверх (Соль5 -> Ре6) с умеренным затуханием без потери громкости
                    notes = listOf(
                        ToneNoteSpec(
                            frequencyHz = 783.99,
                            overtoneRatio = 2.76,
                            overtoneAmplitude = 0.28,
                            durationMs = 75,
                            exponentialDecayRate = 1.8
                        ),
                        ToneNoteSpec(
                            frequencyHz = 1174.66,
                            overtoneRatio = 2.76,
                            overtoneAmplitude = 0.24,
                            durationMs = 95,
                            exponentialDecayRate = 1.9
                        )
                    ),
                    interNoteGapMs = 12,
                    relativeLoudnessScale = 1.0
                )
                SignalType.RELAXATION -> SignalPatternSpec(
                    // Чистый перелив вниз (Ля5 -> Ми5)
                    notes = listOf(
                        ToneNoteSpec(
                            frequencyHz = 880.00,
                            overtoneRatio = 2.76,
                            overtoneAmplitude = 0.24,
                            durationMs = 75,
                            exponentialDecayRate = 1.8
                        ),
                        ToneNoteSpec(
                            frequencyHz = 659.25,
                            overtoneRatio = 2.76,
                            overtoneAmplitude = 0.26,
                            durationMs = 100,
                            exponentialDecayRate = 1.9
                        )
                    ),
                    interNoteGapMs = 12,
                    relativeLoudnessScale = 1.0
                )
                SignalType.PAUSED -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(
                            frequencyHz = 659.25,
                            overtoneRatio = 2.5,
                            overtoneAmplitude = 0.22,
                            durationMs = 65,
                            exponentialDecayRate = 1.8
                        ),
                        ToneNoteSpec(
                            frequencyHz = 523.25,
                            overtoneRatio = 2.5,
                            overtoneAmplitude = 0.24,
                            durationMs = 85,
                            exponentialDecayRate = 1.8
                        )
                    ),
                    interNoteGapMs = 12,
                    relativeLoudnessScale = 0.92
                )
                SignalType.SET_COMPLETED -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 659.25, overtoneRatio = 2.76, overtoneAmplitude = 0.25, durationMs = 65, exponentialDecayRate = 1.7),
                        ToneNoteSpec(frequencyHz = 783.99, overtoneRatio = 2.76, overtoneAmplitude = 0.25, durationMs = 65, exponentialDecayRate = 1.7),
                        ToneNoteSpec(frequencyHz = 1046.50, overtoneRatio = 2.76, overtoneAmplitude = 0.25, durationMs = 110, exponentialDecayRate = 1.9)
                    ),
                    interNoteGapMs = 10,
                    relativeLoudnessScale = 1.0
                )
                SignalType.GROUP_COMPLETED -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 587.33, overtoneRatio = 2.76, overtoneAmplitude = 0.25, durationMs = 60, exponentialDecayRate = 1.7),
                        ToneNoteSpec(frequencyHz = 783.99, overtoneRatio = 2.76, overtoneAmplitude = 0.25, durationMs = 60, exponentialDecayRate = 1.7),
                        ToneNoteSpec(frequencyHz = 987.77, overtoneRatio = 2.76, overtoneAmplitude = 0.25, durationMs = 65, exponentialDecayRate = 1.7),
                        ToneNoteSpec(frequencyHz = 1318.51, overtoneRatio = 2.76, overtoneAmplitude = 0.25, durationMs = 115, exponentialDecayRate = 2.0)
                    ),
                    interNoteGapMs = 10,
                    relativeLoudnessScale = 1.0
                )
                SignalType.METRONOME_TICK -> resolveMetronomePattern(metronomeStyle)
            }

            SoundPack.CRISP -> when (signal) {
                SignalType.PREP_TICK -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(
                            frequencyHz = 783.99,
                            overtoneRatio = 3.0,
                            overtoneAmplitude = 0.18,
                            durationMs = 50
                        )
                    ),
                    interNoteGapMs = 0,
                    relativeLoudnessScale = 0.82
                )
                SignalType.CONTRACTION -> SignalPatternSpec(
                    // Отчётливый короткий дуплет вверх (740 Гц -> 1108 Гц)
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 739.99, overtoneRatio = 3.0, overtoneAmplitude = 0.20, durationMs = 65),
                        ToneNoteSpec(frequencyHz = 1108.73, overtoneRatio = 3.0, overtoneAmplitude = 0.20, durationMs = 85)
                    ),
                    interNoteGapMs = 12,
                    relativeLoudnessScale = 1.0
                )
                SignalType.RELAXATION -> SignalPatternSpec(
                    // Отчётливый короткий дуплет вниз (830 Гц -> 587.33 Гц)
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 830.61, overtoneRatio = 3.0, overtoneAmplitude = 0.18, durationMs = 65),
                        ToneNoteSpec(frequencyHz = 587.33, overtoneRatio = 3.0, overtoneAmplitude = 0.22, durationMs = 90)
                    ),
                    interNoteGapMs = 12,
                    relativeLoudnessScale = 1.0
                )
                SignalType.PAUSED -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 622.25, overtoneRatio = 3.0, overtoneAmplitude = 0.16, durationMs = 60),
                        ToneNoteSpec(frequencyHz = 493.88, overtoneRatio = 3.0, overtoneAmplitude = 0.18, durationMs = 80)
                    ),
                    interNoteGapMs = 12,
                    relativeLoudnessScale = 0.92
                )
                SignalType.SET_COMPLETED -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 659.25, overtoneRatio = 3.0, overtoneAmplitude = 0.18, durationMs = 55),
                        ToneNoteSpec(frequencyHz = 830.61, overtoneRatio = 3.0, overtoneAmplitude = 0.18, durationMs = 55),
                        ToneNoteSpec(frequencyHz = 987.77, overtoneRatio = 3.0, overtoneAmplitude = 0.18, durationMs = 95)
                    ),
                    interNoteGapMs = 10,
                    relativeLoudnessScale = 1.0
                )
                SignalType.GROUP_COMPLETED -> SignalPatternSpec(
                    notes = listOf(
                        ToneNoteSpec(frequencyHz = 659.25, overtoneRatio = 3.0, overtoneAmplitude = 0.18, durationMs = 50),
                        ToneNoteSpec(frequencyHz = 830.61, overtoneRatio = 3.0, overtoneAmplitude = 0.18, durationMs = 50),
                        ToneNoteSpec(frequencyHz = 987.77, overtoneRatio = 3.0, overtoneAmplitude = 0.18, durationMs = 55),
                        ToneNoteSpec(frequencyHz = 1318.51, overtoneRatio = 3.0, overtoneAmplitude = 0.18, durationMs = 105)
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
                        frequencyHz = 480.0,
                        overtoneRatio = 2.0,
                        overtoneAmplitude = 0.15,
                        durationMs = 30,
                        exponentialDecayRate = 2.4
                    )
                ),
                interNoteGapMs = 0,
                relativeLoudnessScale = 0.42
            )
            MetronomeTickStyle.CRISP -> SignalPatternSpec(
                notes = listOf(
                    ToneNoteSpec(
                        frequencyHz = 920.0,
                        overtoneRatio = 2.0,
                        overtoneAmplitude = 0.22,
                        durationMs = 22,
                        exponentialDecayRate = 2.8
                    )
                ),
                interNoteGapMs = 0,
                relativeLoudnessScale = 0.42
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
            // Метроном всегда заметно тише команд фаз, даже если ползунок метронома выкручен до 100%
            val maxAllowedMetro = if (clampedSignal > 0.05f) {
                (clampedSignal * 0.65f).coerceAtLeast(0.08f)
            } else {
                clampedMetro * 0.55f
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
     * Генерирует 16-битный моно PCM-буфер со сглаженными косинусными фронтами (raised-cosine window),
     * исключающими щелчки в начале и конце каждой ноты, и выравнивает среднеквадратичную громкость (RMS)
     * между командами, чтобы внутри сессии не возникало провала громкости после стартового сигнала.
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

            // Плавный косинусный вход (8 мс) и выход (12 мс) без скачка амплитуды
            val attackSamples = ((sampleRate * 8) / 1000).coerceIn(1, (noteSamples / 3).coerceAtLeast(1))
            val releaseSamples = ((sampleRate * 12) / 1000).coerceIn(1, (noteSamples / 2).coerceAtLeast(1))
            val normFactor = 1.0 + note.overtoneAmplitude

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

        // Выравнивание RMS по целевому уровню (с мягким ограничением пиков без клиппинга)
        val rawRms = if (activeSamplesCount > 0) {
            sqrt(activeSamplesSumSq / activeSamplesCount.toDouble()).coerceAtLeast(0.05)
        } else {
            0.5
        }
        val targetRms = TARGET_COMMAND_RMS * pattern.relativeLoudnessScale * clampedVol.toDouble()
        val rmsGain = (targetRms / rawRms).coerceAtMost(0.92 * clampedVol.toDouble())

        val pcm = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val scaled = (rawFloat[i] * rmsGain).coerceIn(-0.92, 0.92)
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
