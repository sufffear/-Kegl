package com.example.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Параметры отдельной ноты/импульса в локальном синтезаторе сигналов.
 * Поддерживает:
 * - плавное скольжение частоты (`frequencyHz -> endFrequencyHz`) для живого «подъёма» на сжатии и «выдоха» на расслаблении;
 * - настраиваемую атаку (`attackMs`) и затухание (`releaseMs`) для мягких бархатных и звонких профилей;
 * - суб-октавный тёплый тон (`subHarmonicAmplitude`, 0.5x частоты) для глубокого обволакивающего тембра отдыха;
 * - 2-ю и 3-ю гармоники для ясности и читаемости на любом динамике.
 */
data class ToneNoteSpec(
    val frequencyHz: Double,
    val endFrequencyHz: Double = frequencyHz,
    val overtoneRatio: Double = 2.0,
    val overtoneAmplitude: Double = 0.35,
    val thirdHarmonicRatio: Double = 3.0,
    val thirdHarmonicAmplitude: Double = 0.15,
    val subHarmonicAmplitude: Double = 0.0,
    val durationMs: Int,
    val attackMs: Int = 6,
    val releaseMs: Int = 12,
    val exponentialDecayRate: Double = 0.6,
    val noteGain: Double = 1.0
)

/**
 * Спецификация звукового паттерна сигнала.
 * `relativeLoudnessScale` задаёт целевую относительную громкость сигнала внутри набора:
 * - Основные команды (`CONTRACTION`, `RELAXATION`, `SET_COMPLETED`, `GROUP_COMPLETED`) выровнены по RMS (`1.0`).
 * - Сигнал подготовки (`PREP_TICK`) чуть мягче (`0.84`), чтобы отсчёт перед стартом не перекрывал рабочую фазу.
 * - Метроном (`METRONOME_TICK`) отчётливый, но деликатный (`0.46`).
 */
data class SignalPatternSpec(
    val notes: List<ToneNoteSpec>,
    val interNoteGapMs: Int = 10,
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
 * Генератор 6 звуковых профилей (4 мягких и приятных + 2 звонких) с кардинальным различием
 * между командами «Удержание» (`CONTRACTION`) и «Отдых» (`RELAXATION`):
 *
 * 1. `CONTRACTION` («Сжать / Удержание»):
 *    - Ритмический рисунок: **3 бодрых восходящих импульса** («Та-та-ТАМ ▲», ~260 мс);
 *    - Регистр и направление: строгий подъём вверх в светлый регистр (587 → 1568 Гц) с микро-лифтом частоты вверх;
 *    - Тембр: собранная атака (5–9 мс), светлые мажорные обертоны без суб-баса.
 *
 * 2. `RELAXATION` («Расслабить / Отдых»):
 *    - Ритмический рисунок: **2 протяжных, плавно перетекающих певучих тона** («Тууу-ууух ▼», ~335 мс);
 *    - Регистр и направление: глубокий бархатный спуск вниз в низкий регистр (523 → 261..349 Гц) с плавным глиссандо выдоха;
 *    - Тембр: мягкая шёлковая атака (18–24 мс), долгий плавный спад (34–46 мс) и насыщенный суб-октавный бас (130–220 Гц).
 */
object SoundSynthesisSpec {

    const val SAMPLE_RATE_HZ = 24_000
    const val MIN_SUPPORTED_PHASE_MS = 1_000

    private const val TARGET_COMMAND_RMS = 0.52

    fun resolveSignalPattern(
        pack: SoundPack,
        signal: SignalType,
        metronomeStyle: MetronomeTickStyle = MetronomeTickStyle.SOFT
    ): SignalPatternSpec {
        if (signal == SignalType.METRONOME_TICK) {
            return resolveMetronomePattern(metronomeStyle)
        }

        return when (pack) {
            SoundPack.VELVET_BOWL -> buildVelvetBowlPattern(signal, metronomeStyle)
            SoundPack.WARM_MARIMBA -> buildWarmMarimbaPattern(signal, metronomeStyle)
            SoundPack.HARP_BREEZE -> buildHarpBreezePattern(signal, metronomeStyle)
            SoundPack.SOFT -> buildSoftVelvetPattern(signal, metronomeStyle)
            SoundPack.CHIMES -> buildChimesPattern(signal, metronomeStyle)
            SoundPack.CRISP -> buildCrispProPattern(signal, metronomeStyle)
        }
    }

    /**
     * 1. «Тибетская чаша» (`VELVET_BOWL`) — мягкий медитативный дзен-профиль.
     * Сжатие: 3 светлых колокольчика вверх (F5 698 Гц -> A5 880 Гц -> D6 1175 Гц).
     * Отдых: 2 глубоких певучих выдоха чаши вниз (C5 523 Гц -> F4 349->329 Гц + суб-октава 174 Гц).
     */
    private fun buildVelvetBowlPattern(
        signal: SignalType,
        metronomeStyle: MetronomeTickStyle
    ): SignalPatternSpec = when (signal) {
        SignalType.PREP_TICK -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(
                    frequencyHz = 587.33, // D5 — мягкое касание чаши
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.28,
                    thirdHarmonicRatio = 2.75,
                    thirdHarmonicAmplitude = 0.10,
                    subHarmonicAmplitude = 0.18,
                    durationMs = 68,
                    attackMs = 12,
                    releaseMs = 22,
                    exponentialDecayRate = 0.55
                )
            ),
            interNoteGapMs = 0,
            relativeLoudnessScale = 0.84
        )
        SignalType.CONTRACTION -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(
                    frequencyHz = 698.46, // F5
                    endFrequencyHz = 710.0,
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.36,
                    thirdHarmonicRatio = 3.0,
                    thirdHarmonicAmplitude = 0.16,
                    durationMs = 66,
                    attackMs = 7,
                    releaseMs = 14,
                    exponentialDecayRate = 0.45
                ),
                ToneNoteSpec(
                    frequencyHz = 880.00, // A5
                    endFrequencyHz = 896.0,
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.38,
                    thirdHarmonicRatio = 3.0,
                    thirdHarmonicAmplitude = 0.18,
                    durationMs = 66,
                    attackMs = 7,
                    releaseMs = 14,
                    exponentialDecayRate = 0.45
                ),
                ToneNoteSpec(
                    frequencyHz = 1174.66, // D6 — светлая вершина
                    endFrequencyHz = 1205.0,
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.40,
                    thirdHarmonicRatio = 3.0,
                    thirdHarmonicAmplitude = 0.20,
                    durationMs = 108,
                    attackMs = 7,
                    releaseMs = 18,
                    exponentialDecayRate = 0.40
                )
            ),
            interNoteGapMs = 10,
            relativeLoudnessScale = 1.0
        )
        SignalType.RELAXATION -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(
                    frequencyHz = 523.25, // C5 -> плавный спуск к A4
                    endFrequencyHz = 466.16,
                    overtoneRatio = 1.5,
                    overtoneAmplitude = 0.22,
                    thirdHarmonicRatio = 2.0,
                    thirdHarmonicAmplitude = 0.06,
                    subHarmonicAmplitude = 0.42,
                    durationMs = 132,
                    attackMs = 20,
                    releaseMs = 30,
                    exponentialDecayRate = 0.35
                ),
                ToneNoteSpec(
                    frequencyHz = 349.23, // F4 -> глубокий выдох к E4 (329 Гц + суб-бас 165 Гц)
                    endFrequencyHz = 329.63,
                    overtoneRatio = 1.5,
                    overtoneAmplitude = 0.18,
                    thirdHarmonicRatio = 2.0,
                    thirdHarmonicAmplitude = 0.04,
                    subHarmonicAmplitude = 0.50,
                    durationMs = 198,
                    attackMs = 22,
                    releaseMs = 42,
                    exponentialDecayRate = 0.35
                )
            ),
            interNoteGapMs = 6,
            relativeLoudnessScale = 1.0
        )
        SignalType.PAUSED -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(frequencyHz = 523.25, endFrequencyHz = 493.88, overtoneRatio = 2.0, overtoneAmplitude = 0.24, thirdHarmonicAmplitude = 0.08, subHarmonicAmplitude = 0.30, durationMs = 80, attackMs = 14, releaseMs = 22, exponentialDecayRate = 0.45),
                ToneNoteSpec(frequencyHz = 392.00, endFrequencyHz = 369.99, overtoneRatio = 2.0, overtoneAmplitude = 0.20, thirdHarmonicAmplitude = 0.06, subHarmonicAmplitude = 0.36, durationMs = 110, attackMs = 16, releaseMs = 28, exponentialDecayRate = 0.45)
            ),
            interNoteGapMs = 10,
            relativeLoudnessScale = 0.94
        )
        SignalType.SET_COMPLETED -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(frequencyHz = 587.33, overtoneRatio = 2.0, overtoneAmplitude = 0.30, thirdHarmonicAmplitude = 0.12, subHarmonicAmplitude = 0.20, durationMs = 74, attackMs = 10, releaseMs = 18, exponentialDecayRate = 0.45),
                ToneNoteSpec(frequencyHz = 739.99, overtoneRatio = 2.0, overtoneAmplitude = 0.30, thirdHarmonicAmplitude = 0.12, subHarmonicAmplitude = 0.18, durationMs = 74, attackMs = 10, releaseMs = 18, exponentialDecayRate = 0.45),
                ToneNoteSpec(frequencyHz = 880.00, overtoneRatio = 2.0, overtoneAmplitude = 0.32, thirdHarmonicAmplitude = 0.14, subHarmonicAmplitude = 0.18, durationMs = 122, attackMs = 10, releaseMs = 24, exponentialDecayRate = 0.40)
            ),
            interNoteGapMs = 10,
            relativeLoudnessScale = 1.0
        )
        SignalType.GROUP_COMPLETED -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(frequencyHz = 587.33, overtoneRatio = 2.0, overtoneAmplitude = 0.30, thirdHarmonicAmplitude = 0.12, subHarmonicAmplitude = 0.18, durationMs = 62, attackMs = 9, releaseMs = 15, exponentialDecayRate = 0.45),
                ToneNoteSpec(frequencyHz = 739.99, overtoneRatio = 2.0, overtoneAmplitude = 0.30, thirdHarmonicAmplitude = 0.12, subHarmonicAmplitude = 0.16, durationMs = 62, attackMs = 9, releaseMs = 15, exponentialDecayRate = 0.45),
                ToneNoteSpec(frequencyHz = 880.00, overtoneRatio = 2.0, overtoneAmplitude = 0.32, thirdHarmonicAmplitude = 0.14, subHarmonicAmplitude = 0.14, durationMs = 66, attackMs = 9, releaseMs = 16, exponentialDecayRate = 0.45),
                ToneNoteSpec(frequencyHz = 1174.66, overtoneRatio = 2.0, overtoneAmplitude = 0.34, thirdHarmonicAmplitude = 0.14, subHarmonicAmplitude = 0.14, durationMs = 116, attackMs = 9, releaseMs = 24, exponentialDecayRate = 0.40)
            ),
            interNoteGapMs = 8,
            relativeLoudnessScale = 1.0
        )
        SignalType.METRONOME_TICK -> resolveMetronomePattern(metronomeStyle)
    }

    /**
     * 2. «Тёплая маримба» (`WARM_MARIMBA`) — уютный деревянный перкуссионный профиль.
     * Сжатие: упругая тройная дробь клавиш вверх (G5 784 Гц -> B5 988 Гц -> E6 1318.5 Гц).
     * Отдых: 2 мягких низких бамбуковых выдоха вниз (B4 494 Гц -> E4 329->311 Гц + суб-бас).
     */
    private fun buildWarmMarimbaPattern(
        signal: SignalType,
        metronomeStyle: MetronomeTickStyle
    ): SignalPatternSpec = when (signal) {
        SignalType.PREP_TICK -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(
                    frequencyHz = 659.25, // E5 — деревянный клик маримбы
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.24,
                    thirdHarmonicRatio = 4.0,
                    thirdHarmonicAmplitude = 0.12,
                    subHarmonicAmplitude = 0.15,
                    durationMs = 64,
                    attackMs = 8,
                    releaseMs = 18,
                    exponentialDecayRate = 0.70
                )
            ),
            interNoteGapMs = 0,
            relativeLoudnessScale = 0.84
        )
        SignalType.CONTRACTION -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(
                    frequencyHz = 783.99, // G5
                    endFrequencyHz = 798.0,
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.34,
                    thirdHarmonicRatio = 4.0,
                    thirdHarmonicAmplitude = 0.18,
                    durationMs = 64,
                    attackMs = 6,
                    releaseMs = 12,
                    exponentialDecayRate = 0.60
                ),
                ToneNoteSpec(
                    frequencyHz = 987.77, // B5
                    endFrequencyHz = 1005.0,
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.36,
                    thirdHarmonicRatio = 4.0,
                    thirdHarmonicAmplitude = 0.18,
                    durationMs = 64,
                    attackMs = 6,
                    releaseMs = 12,
                    exponentialDecayRate = 0.60
                ),
                ToneNoteSpec(
                    frequencyHz = 1318.51, // E6
                    endFrequencyHz = 1348.0,
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.38,
                    thirdHarmonicRatio = 3.0,
                    thirdHarmonicAmplitude = 0.18,
                    durationMs = 106,
                    attackMs = 6,
                    releaseMs = 16,
                    exponentialDecayRate = 0.50
                )
            ),
            interNoteGapMs = 10,
            relativeLoudnessScale = 1.0
        )
        SignalType.RELAXATION -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(
                    frequencyHz = 493.88, // B4 -> G4
                    endFrequencyHz = 440.00,
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.18,
                    thirdHarmonicRatio = 3.0,
                    thirdHarmonicAmplitude = 0.05,
                    subHarmonicAmplitude = 0.44,
                    durationMs = 130,
                    attackMs = 18,
                    releaseMs = 28,
                    exponentialDecayRate = 0.40
                ),
                ToneNoteSpec(
                    frequencyHz = 329.63, // E4 -> Eb4 (глубокий деревянный бас)
                    endFrequencyHz = 308.00,
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.15,
                    thirdHarmonicRatio = 3.0,
                    thirdHarmonicAmplitude = 0.04,
                    subHarmonicAmplitude = 0.52,
                    durationMs = 196,
                    attackMs = 20,
                    releaseMs = 40,
                    exponentialDecayRate = 0.38
                )
            ),
            interNoteGapMs = 6,
            relativeLoudnessScale = 1.0
        )
        SignalType.PAUSED -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(frequencyHz = 587.33, endFrequencyHz = 554.37, overtoneRatio = 2.0, overtoneAmplitude = 0.24, thirdHarmonicAmplitude = 0.08, subHarmonicAmplitude = 0.25, durationMs = 78, attackMs = 12, releaseMs = 20, exponentialDecayRate = 0.55),
                ToneNoteSpec(frequencyHz = 440.00, endFrequencyHz = 415.30, overtoneRatio = 2.0, overtoneAmplitude = 0.20, thirdHarmonicAmplitude = 0.06, subHarmonicAmplitude = 0.32, durationMs = 108, attackMs = 14, releaseMs = 26, exponentialDecayRate = 0.55)
            ),
            interNoteGapMs = 10,
            relativeLoudnessScale = 0.94
        )
        SignalType.SET_COMPLETED -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(frequencyHz = 659.25, overtoneRatio = 2.0, overtoneAmplitude = 0.28, thirdHarmonicAmplitude = 0.12, subHarmonicAmplitude = 0.18, durationMs = 72, attackMs = 8, releaseMs = 16, exponentialDecayRate = 0.55),
                ToneNoteSpec(frequencyHz = 830.61, overtoneRatio = 2.0, overtoneAmplitude = 0.28, thirdHarmonicAmplitude = 0.12, subHarmonicAmplitude = 0.16, durationMs = 72, attackMs = 8, releaseMs = 16, exponentialDecayRate = 0.55),
                ToneNoteSpec(frequencyHz = 987.77, overtoneRatio = 2.0, overtoneAmplitude = 0.30, thirdHarmonicAmplitude = 0.14, subHarmonicAmplitude = 0.16, durationMs = 120, attackMs = 8, releaseMs = 22, exponentialDecayRate = 0.48)
            ),
            interNoteGapMs = 10,
            relativeLoudnessScale = 1.0
        )
        SignalType.GROUP_COMPLETED -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(frequencyHz = 659.25, overtoneRatio = 2.0, overtoneAmplitude = 0.28, thirdHarmonicAmplitude = 0.12, subHarmonicAmplitude = 0.16, durationMs = 60, attackMs = 8, releaseMs = 14, exponentialDecayRate = 0.55),
                ToneNoteSpec(frequencyHz = 830.61, overtoneRatio = 2.0, overtoneAmplitude = 0.28, thirdHarmonicAmplitude = 0.12, subHarmonicAmplitude = 0.16, durationMs = 60, attackMs = 8, releaseMs = 14, exponentialDecayRate = 0.55),
                ToneNoteSpec(frequencyHz = 987.77, overtoneRatio = 2.0, overtoneAmplitude = 0.30, thirdHarmonicAmplitude = 0.12, subHarmonicAmplitude = 0.14, durationMs = 66, attackMs = 8, releaseMs = 16, exponentialDecayRate = 0.55),
                ToneNoteSpec(frequencyHz = 1318.51, overtoneRatio = 2.0, overtoneAmplitude = 0.32, thirdHarmonicAmplitude = 0.14, subHarmonicAmplitude = 0.12, durationMs = 114, attackMs = 8, releaseMs = 22, exponentialDecayRate = 0.48)
            ),
            interNoteGapMs = 8,
            relativeLoudnessScale = 1.0
        )
        SignalType.METRONOME_TICK -> resolveMetronomePattern(metronomeStyle)
    }

    /**
     * 3. «Шёлковая арфа» (`HARP_BREEZE`) — нежный перебор струн.
     * Сжатие: искристый перебор 3 струн вверх (A5 880 Гц -> C#6 1109 Гц -> E6 1318.5 Гц).
     * Отдых: тёплый певучий выдох виолончели вниз (D5 587 Гц -> G4 392->366 Гц + суб-бас).
     */
    private fun buildHarpBreezePattern(
        signal: SignalType,
        metronomeStyle: MetronomeTickStyle
    ): SignalPatternSpec = when (signal) {
        SignalType.PREP_TICK -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(
                    frequencyHz = 659.25, // E5 — мягкий щипок струны
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.30,
                    thirdHarmonicRatio = 3.0,
                    thirdHarmonicAmplitude = 0.12,
                    subHarmonicAmplitude = 0.16,
                    durationMs = 66,
                    attackMs = 10,
                    releaseMs = 20,
                    exponentialDecayRate = 0.60
                )
            ),
            interNoteGapMs = 0,
            relativeLoudnessScale = 0.84
        )
        SignalType.CONTRACTION -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(
                    frequencyHz = 880.00, // A5
                    endFrequencyHz = 898.0,
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.38,
                    thirdHarmonicRatio = 3.0,
                    thirdHarmonicAmplitude = 0.18,
                    durationMs = 66,
                    attackMs = 7,
                    releaseMs = 14,
                    exponentialDecayRate = 0.50
                ),
                ToneNoteSpec(
                    frequencyHz = 1108.73, // C#6
                    endFrequencyHz = 1130.0,
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.38,
                    thirdHarmonicRatio = 3.0,
                    thirdHarmonicAmplitude = 0.18,
                    durationMs = 66,
                    attackMs = 7,
                    releaseMs = 14,
                    exponentialDecayRate = 0.50
                ),
                ToneNoteSpec(
                    frequencyHz = 1318.51, // E6
                    endFrequencyHz = 1352.0,
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.40,
                    thirdHarmonicRatio = 3.0,
                    thirdHarmonicAmplitude = 0.20,
                    durationMs = 108,
                    attackMs = 7,
                    releaseMs = 18,
                    exponentialDecayRate = 0.45
                )
            ),
            interNoteGapMs = 10,
            relativeLoudnessScale = 1.0
        )
        SignalType.RELAXATION -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(
                    frequencyHz = 587.33, // D5 -> B4
                    endFrequencyHz = 515.0,
                    overtoneRatio = 1.5,
                    overtoneAmplitude = 0.22,
                    thirdHarmonicRatio = 2.0,
                    thirdHarmonicAmplitude = 0.06,
                    subHarmonicAmplitude = 0.40,
                    durationMs = 132,
                    attackMs = 20,
                    releaseMs = 30,
                    exponentialDecayRate = 0.36
                ),
                ToneNoteSpec(
                    frequencyHz = 392.00, // G4 -> F#4
                    endFrequencyHz = 366.0,
                    overtoneRatio = 1.5,
                    overtoneAmplitude = 0.18,
                    thirdHarmonicRatio = 2.0,
                    thirdHarmonicAmplitude = 0.05,
                    subHarmonicAmplitude = 0.48,
                    durationMs = 198,
                    attackMs = 22,
                    releaseMs = 42,
                    exponentialDecayRate = 0.36
                )
            ),
            interNoteGapMs = 6,
            relativeLoudnessScale = 1.0
        )
        SignalType.PAUSED -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(frequencyHz = 587.33, endFrequencyHz = 554.37, overtoneRatio = 2.0, overtoneAmplitude = 0.26, thirdHarmonicAmplitude = 0.08, subHarmonicAmplitude = 0.26, durationMs = 78, attackMs = 14, releaseMs = 22, exponentialDecayRate = 0.50),
                ToneNoteSpec(frequencyHz = 440.00, endFrequencyHz = 415.30, overtoneRatio = 2.0, overtoneAmplitude = 0.22, thirdHarmonicAmplitude = 0.06, subHarmonicAmplitude = 0.34, durationMs = 108, attackMs = 16, releaseMs = 28, exponentialDecayRate = 0.50)
            ),
            interNoteGapMs = 10,
            relativeLoudnessScale = 0.94
        )
        SignalType.SET_COMPLETED -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(frequencyHz = 659.25, overtoneRatio = 2.0, overtoneAmplitude = 0.32, thirdHarmonicAmplitude = 0.12, subHarmonicAmplitude = 0.18, durationMs = 74, attackMs = 10, releaseMs = 18, exponentialDecayRate = 0.48),
                ToneNoteSpec(frequencyHz = 880.00, overtoneRatio = 2.0, overtoneAmplitude = 0.32, thirdHarmonicAmplitude = 0.12, subHarmonicAmplitude = 0.16, durationMs = 74, attackMs = 10, releaseMs = 18, exponentialDecayRate = 0.48),
                ToneNoteSpec(frequencyHz = 1108.73, overtoneRatio = 2.0, overtoneAmplitude = 0.34, thirdHarmonicAmplitude = 0.14, subHarmonicAmplitude = 0.14, durationMs = 122, attackMs = 10, releaseMs = 24, exponentialDecayRate = 0.44)
            ),
            interNoteGapMs = 10,
            relativeLoudnessScale = 1.0
        )
        SignalType.GROUP_COMPLETED -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(frequencyHz = 659.25, overtoneRatio = 2.0, overtoneAmplitude = 0.32, thirdHarmonicAmplitude = 0.12, subHarmonicAmplitude = 0.16, durationMs = 62, attackMs = 9, releaseMs = 15, exponentialDecayRate = 0.48),
                ToneNoteSpec(frequencyHz = 880.00, overtoneRatio = 2.0, overtoneAmplitude = 0.32, thirdHarmonicAmplitude = 0.12, subHarmonicAmplitude = 0.16, durationMs = 62, attackMs = 9, releaseMs = 15, exponentialDecayRate = 0.48),
                ToneNoteSpec(frequencyHz = 1108.73, overtoneRatio = 2.0, overtoneAmplitude = 0.34, thirdHarmonicAmplitude = 0.14, subHarmonicAmplitude = 0.14, durationMs = 66, attackMs = 9, releaseMs = 16, exponentialDecayRate = 0.48),
                ToneNoteSpec(frequencyHz = 1318.51, overtoneRatio = 2.0, overtoneAmplitude = 0.36, thirdHarmonicAmplitude = 0.14, subHarmonicAmplitude = 0.12, durationMs = 116, attackMs = 9, releaseMs = 24, exponentialDecayRate = 0.44)
            ),
            interNoteGapMs = 8,
            relativeLoudnessScale = 1.0
        )
        SignalType.METRONOME_TICK -> resolveMetronomePattern(metronomeStyle)
    }

    /**
     * 4. «Мягкий бархат» (`SOFT`) — деликатный флейтово-бархатный профиль.
     * Сжатие: 3 упругих светлых импульса вверх (G5 784 Гц -> C6 1046.5 Гц -> E6 1318.5 Гц).
     * Отдых: 2 глубоких бархатных тона вниз (C5 523 Гц -> F4 349->325 Гц + суб-бас).
     */
    private fun buildSoftVelvetPattern(
        signal: SignalType,
        metronomeStyle: MetronomeTickStyle
    ): SignalPatternSpec = when (signal) {
        SignalType.PREP_TICK -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(
                    frequencyHz = 659.25, // E5 — мягкий флейтовый тон
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.26,
                    thirdHarmonicRatio = 3.0,
                    thirdHarmonicAmplitude = 0.10,
                    subHarmonicAmplitude = 0.16,
                    durationMs = 66,
                    attackMs = 12,
                    releaseMs = 20,
                    exponentialDecayRate = 0.50
                )
            ),
            interNoteGapMs = 0,
            relativeLoudnessScale = 0.84
        )
        SignalType.CONTRACTION -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(
                    frequencyHz = 783.99, // G5
                    endFrequencyHz = 800.0,
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.36,
                    thirdHarmonicRatio = 3.0,
                    thirdHarmonicAmplitude = 0.16,
                    durationMs = 66,
                    attackMs = 7,
                    releaseMs = 14,
                    exponentialDecayRate = 0.45
                ),
                ToneNoteSpec(
                    frequencyHz = 1046.50, // C6
                    endFrequencyHz = 1066.0,
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.38,
                    thirdHarmonicRatio = 3.0,
                    thirdHarmonicAmplitude = 0.18,
                    durationMs = 66,
                    attackMs = 7,
                    releaseMs = 14,
                    exponentialDecayRate = 0.45
                ),
                ToneNoteSpec(
                    frequencyHz = 1318.51, // E6
                    endFrequencyHz = 1350.0,
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.40,
                    thirdHarmonicRatio = 3.0,
                    thirdHarmonicAmplitude = 0.18,
                    durationMs = 108,
                    attackMs = 7,
                    releaseMs = 18,
                    exponentialDecayRate = 0.42
                )
            ),
            interNoteGapMs = 10,
            relativeLoudnessScale = 1.0
        )
        SignalType.RELAXATION -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(
                    frequencyHz = 523.25, // C5 -> A4
                    endFrequencyHz = 466.16,
                    overtoneRatio = 1.5,
                    overtoneAmplitude = 0.20,
                    thirdHarmonicRatio = 2.0,
                    thirdHarmonicAmplitude = 0.05,
                    subHarmonicAmplitude = 0.44,
                    durationMs = 132,
                    attackMs = 20,
                    releaseMs = 30,
                    exponentialDecayRate = 0.36
                ),
                ToneNoteSpec(
                    frequencyHz = 349.23, // F4 -> E4
                    endFrequencyHz = 325.00,
                    overtoneRatio = 1.5,
                    overtoneAmplitude = 0.16,
                    thirdHarmonicRatio = 2.0,
                    thirdHarmonicAmplitude = 0.04,
                    subHarmonicAmplitude = 0.52,
                    durationMs = 198,
                    attackMs = 22,
                    releaseMs = 42,
                    exponentialDecayRate = 0.35
                )
            ),
            interNoteGapMs = 6,
            relativeLoudnessScale = 1.0
        )
        SignalType.PAUSED -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(frequencyHz = 587.33, endFrequencyHz = 554.37, overtoneRatio = 2.0, overtoneAmplitude = 0.26, thirdHarmonicAmplitude = 0.08, subHarmonicAmplitude = 0.28, durationMs = 78, attackMs = 14, releaseMs = 22, exponentialDecayRate = 0.48),
                ToneNoteSpec(frequencyHz = 440.00, endFrequencyHz = 415.30, overtoneRatio = 2.0, overtoneAmplitude = 0.22, thirdHarmonicAmplitude = 0.06, subHarmonicAmplitude = 0.34, durationMs = 108, attackMs = 16, releaseMs = 28, exponentialDecayRate = 0.48)
            ),
            interNoteGapMs = 10,
            relativeLoudnessScale = 0.94
        )
        SignalType.SET_COMPLETED -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(frequencyHz = 659.25, overtoneRatio = 2.0, overtoneAmplitude = 0.30, thirdHarmonicAmplitude = 0.12, subHarmonicAmplitude = 0.18, durationMs = 74, attackMs = 10, releaseMs = 18, exponentialDecayRate = 0.46),
                ToneNoteSpec(frequencyHz = 783.99, overtoneRatio = 2.0, overtoneAmplitude = 0.30, thirdHarmonicAmplitude = 0.12, subHarmonicAmplitude = 0.16, durationMs = 74, attackMs = 10, releaseMs = 18, exponentialDecayRate = 0.46),
                ToneNoteSpec(frequencyHz = 1046.50, overtoneRatio = 2.0, overtoneAmplitude = 0.32, thirdHarmonicAmplitude = 0.14, subHarmonicAmplitude = 0.14, durationMs = 122, attackMs = 10, releaseMs = 24, exponentialDecayRate = 0.42)
            ),
            interNoteGapMs = 10,
            relativeLoudnessScale = 1.0
        )
        SignalType.GROUP_COMPLETED -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(frequencyHz = 659.25, overtoneRatio = 2.0, overtoneAmplitude = 0.30, thirdHarmonicAmplitude = 0.12, subHarmonicAmplitude = 0.16, durationMs = 62, attackMs = 9, releaseMs = 15, exponentialDecayRate = 0.46),
                ToneNoteSpec(frequencyHz = 783.99, overtoneRatio = 2.0, overtoneAmplitude = 0.30, thirdHarmonicAmplitude = 0.12, subHarmonicAmplitude = 0.16, durationMs = 62, attackMs = 9, releaseMs = 15, exponentialDecayRate = 0.46),
                ToneNoteSpec(frequencyHz = 987.77, overtoneRatio = 2.0, overtoneAmplitude = 0.32, thirdHarmonicAmplitude = 0.14, subHarmonicAmplitude = 0.14, durationMs = 66, attackMs = 9, releaseMs = 16, exponentialDecayRate = 0.46),
                ToneNoteSpec(frequencyHz = 1318.51, overtoneRatio = 2.0, overtoneAmplitude = 0.34, thirdHarmonicAmplitude = 0.14, subHarmonicAmplitude = 0.12, durationMs = 116, attackMs = 9, releaseMs = 24, exponentialDecayRate = 0.42)
            ),
            interNoteGapMs = 8,
            relativeLoudnessScale = 1.0
        )
        SignalType.METRONOME_TICK -> resolveMetronomePattern(metronomeStyle)
    }

    /**
     * 5. «Кристалл» (`CHIMES`) — звонкий хрустальный перезвон с тёплым карильоном на отдых.
     * Сжатие: 3 искристых колокольчика вверх (A5 880 Гц -> D6 1175 Гц -> F#6 1480 Гц).
     * Отдых: 2 мягких карильона вниз в тёплом регистре (E5 659 Гц -> A4 440->415 Гц + суб-бас).
     */
    private fun buildChimesPattern(
        signal: SignalType,
        metronomeStyle: MetronomeTickStyle
    ): SignalPatternSpec = when (signal) {
        SignalType.PREP_TICK -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(
                    frequencyHz = 880.00, // A5
                    overtoneRatio = 2.4,
                    overtoneAmplitude = 0.38,
                    thirdHarmonicRatio = 3.0,
                    thirdHarmonicAmplitude = 0.18,
                    durationMs = 60,
                    attackMs = 6,
                    releaseMs = 14,
                    exponentialDecayRate = 0.75
                )
            ),
            interNoteGapMs = 0,
            relativeLoudnessScale = 0.84
        )
        SignalType.CONTRACTION -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(
                    frequencyHz = 880.00, // A5
                    endFrequencyHz = 898.0,
                    overtoneRatio = 2.4,
                    overtoneAmplitude = 0.42,
                    thirdHarmonicRatio = 3.0,
                    thirdHarmonicAmplitude = 0.22,
                    durationMs = 64,
                    attackMs = 5,
                    releaseMs = 12,
                    exponentialDecayRate = 0.65
                ),
                ToneNoteSpec(
                    frequencyHz = 1174.66, // D6
                    endFrequencyHz = 1198.0,
                    overtoneRatio = 2.4,
                    overtoneAmplitude = 0.42,
                    thirdHarmonicRatio = 3.0,
                    thirdHarmonicAmplitude = 0.22,
                    durationMs = 64,
                    attackMs = 5,
                    releaseMs = 12,
                    exponentialDecayRate = 0.65
                ),
                ToneNoteSpec(
                    frequencyHz = 1479.98, // F#6
                    endFrequencyHz = 1515.0,
                    overtoneRatio = 2.4,
                    overtoneAmplitude = 0.44,
                    thirdHarmonicRatio = 3.0,
                    thirdHarmonicAmplitude = 0.22,
                    durationMs = 104,
                    attackMs = 5,
                    releaseMs = 16,
                    exponentialDecayRate = 0.55
                )
            ),
            interNoteGapMs = 10,
            relativeLoudnessScale = 1.0
        )
        SignalType.RELAXATION -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(
                    frequencyHz = 659.25, // E5 -> C#5
                    endFrequencyHz = 587.33,
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.24,
                    thirdHarmonicRatio = 2.5,
                    thirdHarmonicAmplitude = 0.08,
                    subHarmonicAmplitude = 0.36,
                    durationMs = 130,
                    attackMs = 18,
                    releaseMs = 28,
                    exponentialDecayRate = 0.45
                ),
                ToneNoteSpec(
                    frequencyHz = 440.00, // A4 -> G#4
                    endFrequencyHz = 412.00,
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.20,
                    thirdHarmonicRatio = 2.5,
                    thirdHarmonicAmplitude = 0.06,
                    subHarmonicAmplitude = 0.44,
                    durationMs = 194,
                    attackMs = 20,
                    releaseMs = 38,
                    exponentialDecayRate = 0.42
                )
            ),
            interNoteGapMs = 6,
            relativeLoudnessScale = 1.0
        )
        SignalType.PAUSED -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(frequencyHz = 783.99, endFrequencyHz = 740.0, overtoneRatio = 2.2, overtoneAmplitude = 0.32, thirdHarmonicAmplitude = 0.14, subHarmonicAmplitude = 0.20, durationMs = 74, attackMs = 10, releaseMs = 18, exponentialDecayRate = 0.65),
                ToneNoteSpec(frequencyHz = 523.25, endFrequencyHz = 493.88, overtoneRatio = 2.2, overtoneAmplitude = 0.28, thirdHarmonicAmplitude = 0.12, subHarmonicAmplitude = 0.26, durationMs = 102, attackMs = 12, releaseMs = 24, exponentialDecayRate = 0.65)
            ),
            interNoteGapMs = 10,
            relativeLoudnessScale = 0.94
        )
        SignalType.SET_COMPLETED -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(frequencyHz = 880.00, overtoneRatio = 2.4, overtoneAmplitude = 0.38, thirdHarmonicAmplitude = 0.18, durationMs = 70, attackMs = 7, releaseMs = 15, exponentialDecayRate = 0.60),
                ToneNoteSpec(frequencyHz = 1108.73, overtoneRatio = 2.4, overtoneAmplitude = 0.38, thirdHarmonicAmplitude = 0.18, durationMs = 70, attackMs = 7, releaseMs = 15, exponentialDecayRate = 0.60),
                ToneNoteSpec(frequencyHz = 1318.51, overtoneRatio = 2.4, overtoneAmplitude = 0.40, thirdHarmonicAmplitude = 0.18, durationMs = 115, attackMs = 7, releaseMs = 20, exponentialDecayRate = 0.55)
            ),
            interNoteGapMs = 10,
            relativeLoudnessScale = 1.0
        )
        SignalType.GROUP_COMPLETED -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(frequencyHz = 880.00, overtoneRatio = 2.4, overtoneAmplitude = 0.38, thirdHarmonicAmplitude = 0.18, durationMs = 60, attackMs = 7, releaseMs = 14, exponentialDecayRate = 0.60),
                ToneNoteSpec(frequencyHz = 1108.73, overtoneRatio = 2.4, overtoneAmplitude = 0.38, thirdHarmonicAmplitude = 0.18, durationMs = 60, attackMs = 7, releaseMs = 14, exponentialDecayRate = 0.60),
                ToneNoteSpec(frequencyHz = 1318.51, overtoneRatio = 2.4, overtoneAmplitude = 0.38, thirdHarmonicAmplitude = 0.18, durationMs = 64, attackMs = 7, releaseMs = 14, exponentialDecayRate = 0.60),
                ToneNoteSpec(frequencyHz = 1760.00, overtoneRatio = 2.4, overtoneAmplitude = 0.40, thirdHarmonicAmplitude = 0.18, durationMs = 112, attackMs = 7, releaseMs = 20, exponentialDecayRate = 0.55)
            ),
            interNoteGapMs = 8,
            relativeLoudnessScale = 1.0
        )
        SignalType.METRONOME_TICK -> resolveMetronomePattern(metronomeStyle)
    }

    /**
     * 6. «Импульс Pro» (`CRISP`) — яркий спортивный профиль, при этом Сжатие и Отдых кардинально разведены:
     * Сжатие: 3 энергичных стартовых импульса вверх (A5 880 Гц -> D6 1175 Гц -> G6 1568 Гц).
     * Отдых: 2 плавных тёплых тона вниз в нижнем регистре (D5 587 Гц -> G4 392->370 Гц + суб-бас).
     */
    private fun buildCrispProPattern(
        signal: SignalType,
        metronomeStyle: MetronomeTickStyle
    ): SignalPatternSpec = when (signal) {
        SignalType.PREP_TICK -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(
                    frequencyHz = 880.00, // A5
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.44,
                    thirdHarmonicRatio = 3.0,
                    thirdHarmonicAmplitude = 0.24,
                    durationMs = 58,
                    attackMs = 5,
                    releaseMs = 12,
                    exponentialDecayRate = 0.55
                )
            ),
            interNoteGapMs = 0,
            relativeLoudnessScale = 0.84
        )
        SignalType.CONTRACTION -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(
                    frequencyHz = 880.00, // A5
                    endFrequencyHz = 900.0,
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.46,
                    thirdHarmonicRatio = 3.0,
                    thirdHarmonicAmplitude = 0.26,
                    durationMs = 64,
                    attackMs = 5,
                    releaseMs = 11,
                    exponentialDecayRate = 0.48
                ),
                ToneNoteSpec(
                    frequencyHz = 1174.66, // D6
                    endFrequencyHz = 1200.0,
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.46,
                    thirdHarmonicRatio = 3.0,
                    thirdHarmonicAmplitude = 0.26,
                    durationMs = 64,
                    attackMs = 5,
                    releaseMs = 11,
                    exponentialDecayRate = 0.48
                ),
                ToneNoteSpec(
                    frequencyHz = 1567.98, // G6
                    endFrequencyHz = 1605.0,
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.46,
                    thirdHarmonicRatio = 3.0,
                    thirdHarmonicAmplitude = 0.24,
                    durationMs = 104,
                    attackMs = 5,
                    releaseMs = 15,
                    exponentialDecayRate = 0.45
                )
            ),
            interNoteGapMs = 10,
            relativeLoudnessScale = 1.0
        )
        SignalType.RELAXATION -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(
                    frequencyHz = 587.33, // D5 -> B4
                    endFrequencyHz = 523.25,
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.28,
                    thirdHarmonicRatio = 3.0,
                    thirdHarmonicAmplitude = 0.10,
                    subHarmonicAmplitude = 0.36,
                    durationMs = 128,
                    attackMs = 16,
                    releaseMs = 26,
                    exponentialDecayRate = 0.42
                ),
                ToneNoteSpec(
                    frequencyHz = 392.00, // G4 -> F#4
                    endFrequencyHz = 368.00,
                    overtoneRatio = 2.0,
                    overtoneAmplitude = 0.24,
                    thirdHarmonicRatio = 3.0,
                    thirdHarmonicAmplitude = 0.08,
                    subHarmonicAmplitude = 0.44,
                    durationMs = 192,
                    attackMs = 18,
                    releaseMs = 36,
                    exponentialDecayRate = 0.40
                )
            ),
            interNoteGapMs = 6,
            relativeLoudnessScale = 1.0
        )
        SignalType.PAUSED -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(frequencyHz = 783.99, endFrequencyHz = 740.0, overtoneRatio = 2.0, overtoneAmplitude = 0.36, thirdHarmonicAmplitude = 0.16, subHarmonicAmplitude = 0.20, durationMs = 72, attackMs = 8, releaseMs = 16, exponentialDecayRate = 0.55),
                ToneNoteSpec(frequencyHz = 523.25, endFrequencyHz = 493.88, overtoneRatio = 2.0, overtoneAmplitude = 0.32, thirdHarmonicAmplitude = 0.14, subHarmonicAmplitude = 0.28, durationMs = 100, attackMs = 10, releaseMs = 22, exponentialDecayRate = 0.55)
            ),
            interNoteGapMs = 10,
            relativeLoudnessScale = 0.94
        )
        SignalType.SET_COMPLETED -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(frequencyHz = 880.00, overtoneRatio = 2.0, overtoneAmplitude = 0.42, thirdHarmonicAmplitude = 0.22, durationMs = 68, attackMs = 6, releaseMs = 14, exponentialDecayRate = 0.50),
                ToneNoteSpec(frequencyHz = 1108.73, overtoneRatio = 2.0, overtoneAmplitude = 0.42, thirdHarmonicAmplitude = 0.22, durationMs = 68, attackMs = 6, releaseMs = 14, exponentialDecayRate = 0.50),
                ToneNoteSpec(frequencyHz = 1318.51, overtoneRatio = 2.0, overtoneAmplitude = 0.44, thirdHarmonicAmplitude = 0.22, durationMs = 112, attackMs = 6, releaseMs = 18, exponentialDecayRate = 0.46)
            ),
            interNoteGapMs = 10,
            relativeLoudnessScale = 1.0
        )
        SignalType.GROUP_COMPLETED -> SignalPatternSpec(
            notes = listOf(
                ToneNoteSpec(frequencyHz = 880.00, overtoneRatio = 2.0, overtoneAmplitude = 0.42, thirdHarmonicAmplitude = 0.22, durationMs = 58, attackMs = 6, releaseMs = 12, exponentialDecayRate = 0.50),
                ToneNoteSpec(frequencyHz = 1108.73, overtoneRatio = 2.0, overtoneAmplitude = 0.42, thirdHarmonicAmplitude = 0.22, durationMs = 58, attackMs = 6, releaseMs = 12, exponentialDecayRate = 0.50),
                ToneNoteSpec(frequencyHz = 1318.51, overtoneRatio = 2.0, overtoneAmplitude = 0.42, thirdHarmonicAmplitude = 0.22, durationMs = 62, attackMs = 6, releaseMs = 14, exponentialDecayRate = 0.50),
                ToneNoteSpec(frequencyHz = 1760.00, overtoneRatio = 2.0, overtoneAmplitude = 0.42, thirdHarmonicAmplitude = 0.20, durationMs = 110, attackMs = 6, releaseMs = 18, exponentialDecayRate = 0.46)
            ),
            interNoteGapMs = 8,
            relativeLoudnessScale = 1.0
        )
        SignalType.METRONOME_TICK -> resolveMetronomePattern(metronomeStyle)
    }

    fun resolveMetronomePattern(style: MetronomeTickStyle): SignalPatternSpec {
        return when (style) {
            MetronomeTickStyle.SOFT -> SignalPatternSpec(
                notes = listOf(
                    ToneNoteSpec(
                        frequencyHz = 740.0,
                        endFrequencyHz = 690.0,
                        overtoneRatio = 2.0,
                        overtoneAmplitude = 0.22,
                        thirdHarmonicRatio = 3.0,
                        thirdHarmonicAmplitude = 0.06,
                        subHarmonicAmplitude = 0.20,
                        durationMs = 30,
                        attackMs = 6,
                        releaseMs = 14,
                        exponentialDecayRate = 1.4
                    )
                ),
                interNoteGapMs = 0,
                relativeLoudnessScale = 0.46
            )
            MetronomeTickStyle.VELVET_WOOD -> SignalPatternSpec(
                notes = listOf(
                    ToneNoteSpec(
                        frequencyHz = 560.0,
                        endFrequencyHz = 520.0,
                        overtoneRatio = 2.0,
                        overtoneAmplitude = 0.18,
                        thirdHarmonicRatio = 4.0,
                        thirdHarmonicAmplitude = 0.08,
                        subHarmonicAmplitude = 0.28,
                        durationMs = 32,
                        attackMs = 6,
                        releaseMs = 15,
                        exponentialDecayRate = 1.5
                    )
                ),
                interNoteGapMs = 0,
                relativeLoudnessScale = 0.46
            )
            MetronomeTickStyle.CRISP -> SignalPatternSpec(
                notes = listOf(
                    ToneNoteSpec(
                        frequencyHz = 1480.0,
                        overtoneRatio = 2.0,
                        overtoneAmplitude = 0.38,
                        thirdHarmonicRatio = 2.5,
                        thirdHarmonicAmplitude = 0.18,
                        durationMs = 25,
                        attackMs = 4,
                        releaseMs = 10,
                        exponentialDecayRate = 1.8
                    )
                ),
                interNoteGapMs = 0,
                relativeLoudnessScale = 0.46
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
     * Оценивает эффективную частоту пересечений нуля (Zero-Crossing Frequency, Гц) активных участков PCM-буфера.
     * Позволяет объективно подтвердить кардинальное спектральное различие между высоким сигналом сжатия
     * и низким бархатным сигналом расслабления.
     */
    fun computeZeroCrossingRateHz(
        pcm: ShortArray,
        sampleRate: Int = SAMPLE_RATE_HZ
    ): Double {
        if (pcm.size < 4) return 0.0
        var crossings = 0
        var activeSamples = 0
        var prevSign = 0
        for (sample in pcm) {
            val v = sample.toInt()
            if (v != 0) {
                activeSamples++
                val curSign = if (v > 0) 1 else -1
                if (prevSign != 0 && curSign != prevSign) {
                    crossings++
                }
                prevSign = curSign
            } else {
                prevSign = 0
            }
        }
        if (activeSamples <= 1) return 0.0
        val activeSeconds = activeSamples.toDouble() / sampleRate.toDouble()
        return (crossings.toDouble() / 2.0) / activeSeconds
    }

    /**
     * Генерирует 16-битный моно PCM-буфер с плавной косинусной огибающей атаки/спада,
     * непрерывным интегрированием фазы при глиссандо частоты и точной двухпроходной калибровкой RMS.
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
        val isActiveSample = BooleanArray(totalSamples)
        var writeIdx = 0

        for ((noteIdx, note) in pattern.notes.withIndex()) {
            val noteSamples = (sampleRate * note.durationMs) / 1000
            if (noteSamples <= 0) continue

            val attackSamples = ((sampleRate * note.attackMs) / 1000)
                .coerceIn(1, (noteSamples / 3).coerceAtLeast(1))
            val releaseSamples = ((sampleRate * note.releaseMs) / 1000)
                .coerceIn(1, (noteSamples / 2).coerceAtLeast(1))
            val normFactor = 1.0 +
                note.overtoneAmplitude +
                note.thirdHarmonicAmplitude +
                note.subHarmonicAmplitude

            var phaseRad = 0.0
            for (s in 0 until noteSamples) {
                if (writeIdx >= rawFloat.size) break

                val progress = s.toDouble() / noteSamples.toDouble()
                val instantFreq = note.frequencyHz + (note.endFrequencyHz - note.frequencyHz) * progress
                phaseRad += (2.0 * PI * instantFreq) / sampleRate.toDouble()

                val env = when {
                    s == 0 || s == noteSamples - 1 -> 0.0
                    s < attackSamples -> 0.5 * (1.0 - cos(PI * s.toDouble() / attackSamples))
                    s >= noteSamples - releaseSamples -> {
                        val relPos = (noteSamples - 1 - s).toDouble() / releaseSamples
                        0.5 * (1.0 - cos(PI * relPos))
                    }
                    else -> 1.0
                }

                val decay = if (note.exponentialDecayRate > 0.0) {
                    exp(-note.exponentialDecayRate * progress)
                } else {
                    1.0
                }

                var wave = sin(phaseRad)
                if (note.subHarmonicAmplitude > 0.0) {
                    wave += note.subHarmonicAmplitude * sin(0.5 * phaseRad)
                }
                if (note.overtoneAmplitude > 0.0 && note.overtoneRatio > 0.0) {
                    wave += note.overtoneAmplitude * sin(note.overtoneRatio * phaseRad)
                }
                if (note.thirdHarmonicAmplitude > 0.0 && note.thirdHarmonicRatio > 0.0) {
                    wave += note.thirdHarmonicAmplitude * sin(note.thirdHarmonicRatio * phaseRad)
                }
                wave = (wave / normFactor) * env * decay * note.noteGain

                rawFloat[writeIdx] = wave
                if (s in 1 until (noteSamples - 1)) {
                    isActiveSample[writeIdx] = true
                }
                writeIdx++
            }

            if (writeIdx > 0 && writeIdx <= rawFloat.size) {
                rawFloat[writeIdx - 1] = 0.0
                isActiveSample[writeIdx - 1] = false
            }

            if (noteIdx < pattern.notes.lastIndex && pattern.interNoteGapMs > 0) {
                val gapSamples = (sampleRate * pattern.interNoteGapMs) / 1000
                writeIdx = (writeIdx + gapSamples).coerceAtMost(rawFloat.size)
            }
        }

        // Проход 1: вычисляем исходный RMS активных отсчётов и применяем мягкое насыщение пиков
        var sumSqPass1 = 0.0
        var activeCount = 0
        for (i in 0 until totalSamples) {
            if (isActiveSample[i]) {
                sumSqPass1 += rawFloat[i] * rawFloat[i]
                activeCount++
            }
        }

        val rawRms = if (activeCount > 0) {
            sqrt(sumSqPass1 / activeCount.toDouble()).coerceAtLeast(0.02)
        } else {
            0.5
        }
        val targetRms = TARGET_COMMAND_RMS * pattern.relativeLoudnessScale * clampedVol.toDouble()
        val pass1Gain = (targetRms / rawRms).coerceAtMost(3.0)

        var sumSqPass2 = 0.0
        for (i in 0 until totalSamples) {
            if (isActiveSample[i]) {
                val v = rawFloat[i] * pass1Gain
                val softLimited = if (abs(v) > 0.82) {
                    sign(v) * (0.82 + 0.15 * tanh((abs(v) - 0.82) / 0.15))
                } else {
                    v
                }
                rawFloat[i] = softLimited
                sumSqPass2 += softLimited * softLimited
            } else {
                rawFloat[i] = 0.0
            }
        }

        // Проход 2: точная калибровка итогового RMS после мягкого лимитера
        val postLimiterRms = if (activeCount > 0) {
            sqrt(sumSqPass2 / activeCount.toDouble()).coerceAtLeast(0.02)
        } else {
            targetRms
        }
        val fineGain = (targetRms / postLimiterRms).coerceIn(0.5, 1.35)

        val pcm = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            if (!isActiveSample[i]) {
                pcm[i] = 0
            } else {
                val scaled = (rawFloat[i] * fineGain).coerceIn(-0.985, 0.985)
                var q = (scaled * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                if (q == 0) {
                    q = if (scaled >= 0.0) 1 else -1
                }
                pcm[i] = q.toShort()
            }
        }

        if (pcm.isNotEmpty()) {
            pcm[0] = 0
            pcm[pcm.lastIndex] = 0
        }
        return pcm
    }
}
