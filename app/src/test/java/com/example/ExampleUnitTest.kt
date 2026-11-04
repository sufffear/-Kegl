package com.example

import com.example.domain.CheckpointTrigger
import com.example.domain.DemoProtocolConfig
import com.example.domain.HeadingLevelRule
import com.example.domain.MarkdownNovelParser
import com.example.domain.MetronomeTickStyle
import com.example.domain.NeutralTestMarkdownFactory
import com.example.domain.NovelBlockType
import com.example.domain.NovelInlineStyle
import com.example.domain.PhaseEngine
import com.example.domain.PhaseEngineSnapshot
import com.example.domain.PhaseType
import com.example.domain.RunStatus
import com.example.domain.SignalType
import com.example.domain.SoundPack
import com.example.domain.SoundSynthesisSpec
import com.example.domain.calculateFreeCycles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ExampleUnitTest {

    @Test
    fun `rapid duplicate start is blocked while session is active`() {
        var nowMs = 1_000L
        val signals = mutableListOf<SignalType>()
        val engine = PhaseEngine(
            clock = { nowMs },
            onSignal = { sig, _, _ -> signals.add(sig) }
        )

        val proto = DemoProtocolConfig(
            id = "test_proto",
            titleRu = "Тест",
            contractionMs = 4_000L,
            relaxationMs = 4_000L,
            repetitions = 2
        )

        assertTrue(engine.start(proto))
        assertEquals(RunStatus.PREPARING, engine.currentSnapshot().status)
        assertFalse(engine.start(proto))
        assertEquals(1, signals.count { it == SignalType.PREP_TICK })
    }

    @Test
    fun `preparation emits 3 signals, does not count as active time, and emits phase command on RUNNING`() {
        var nowMs = 10_000L
        val signals = mutableListOf<SignalType>()
        val engine = PhaseEngine(
            clock = { nowMs },
            onSignal = { sig, _, _ -> signals.add(sig) }
        )

        val proto = DemoProtocolConfig(
            id = "test_prep",
            titleRu = "Тест подготовки",
            contractionMs = 3_000L,
            relaxationMs = 3_000L,
            repetitions = 2
        )

        engine.start(proto)
        assertEquals(listOf(SignalType.PREP_TICK), signals)

        nowMs += 1_000L
        engine.tick()
        assertEquals(0L, engine.currentSnapshot().confirmedActiveMs)
        assertEquals(2, signals.count { it == SignalType.PREP_TICK })

        nowMs += 1_000L
        engine.tick()
        assertEquals(0L, engine.currentSnapshot().confirmedActiveMs)
        assertEquals(3, signals.count { it == SignalType.PREP_TICK })

        nowMs += 1_000L
        val snap = engine.tick()
        assertEquals(RunStatus.RUNNING, snap.status)
        assertEquals(PhaseType.CONTRACTION, snap.currentPhase)
        assertEquals(0L, snap.confirmedActiveMs)
        assertEquals(SignalType.CONTRACTION, signals.last())
    }

    @Test
    fun `pause for 60 seconds does not increase active time and resume continues remaining phase`() {
        var nowMs = 0L
        val signals = mutableListOf<SignalType>()
        val engine = PhaseEngine(
            clock = { nowMs },
            onSignal = { sig, _, _ -> signals.add(sig) }
        )

        val proto = DemoProtocolConfig(
            id = "test_pause",
            titleRu = "Тест паузы",
            contractionMs = 4_000L,
            relaxationMs = 4_000L,
            repetitions = 2
        )

        engine.start(proto)
        repeat(3) {
            nowMs += 1_000L
            engine.tick()
        }
        assertEquals(RunStatus.RUNNING, engine.currentSnapshot().status)

        nowMs += 1_000L
        engine.tick()
        nowMs += 500L
        engine.tick()
        assertEquals(1_500L, engine.currentSnapshot().confirmedActiveMs)
        assertEquals(2_500L, engine.currentSnapshot().phaseRemainingMs)

        val pausedSnap = engine.pause("Тестовая пауза")
        assertEquals(RunStatus.PAUSED, pausedSnap.status)
        assertEquals("Пауза — расслабьтесь", pausedSnap.headlineCommandRu)
        assertEquals(1_500L, pausedSnap.confirmedActiveMs)
        assertEquals(2_500L, pausedSnap.phaseRemainingMs)

        nowMs += 60_000L
        val after60sPause = engine.tick()
        assertEquals(1_500L, after60sPause.confirmedActiveMs)
        assertEquals(2_500L, after60sPause.phaseRemainingMs)

        assertTrue(engine.resume())
        assertEquals(RunStatus.PREPARING, engine.currentSnapshot().status)
        repeat(3) {
            nowMs += 1_000L
            engine.tick()
        }
        val resumedSnap = engine.currentSnapshot()
        assertEquals(RunStatus.RUNNING, resumedSnap.status)
        assertEquals(PhaseType.CONTRACTION, resumedSnap.currentPhase)
        assertEquals(2_500L, resumedSnap.phaseRemainingMs)
        assertEquals(1_500L, resumedSnap.confirmedActiveMs)
    }

    @Test
    fun `cancelling stop confirmation dialog keeps session paused`() {
        var nowMs = 5_000L
        val engine = PhaseEngine(clock = { nowMs })
        engine.start()
        repeat(3) {
            nowMs += 1_000L
            engine.tick()
        }
        assertEquals(RunStatus.RUNNING, engine.currentSnapshot().status)

        val reqSnap = engine.requestStop()
        assertEquals(RunStatus.PAUSED, reqSnap.status)
        assertTrue(reqSnap.isStopDialogVisible)

        val cancelledSnap = engine.cancelStopDialog()
        assertEquals(RunStatus.PAUSED, cancelledSnap.status)
        assertFalse(cancelledSnap.isStopDialogVisible)

        engine.requestStop()
        val stoppedSnap = engine.confirmStop()
        assertEquals(RunStatus.STOPPED, stoppedSnap.status)
        assertFalse(stoppedSnap.isStopDialogVisible)
    }

    @Test
    fun `scheduler delay exceeding threshold transitions to INTERRUPTED without burst signals`() {
        var nowMs = 0L
        val signals = mutableListOf<SignalType>()
        val engine = PhaseEngine(
            clock = { nowMs },
            onSignal = { sig, _, _ -> signals.add(sig) }
        )

        engine.start()
        repeat(3) {
            nowMs += 1_000L
            engine.tick()
        }
        assertEquals(RunStatus.RUNNING, engine.currentSnapshot().status)
        val countBeforeDelay = signals.size

        nowMs += 6_000L
        val snap = engine.tick()
        assertEquals(RunStatus.INTERRUPTED, snap.status)
        assertEquals(0L, snap.confirmedActiveMs)
        assertEquals(countBeforeDelay + 1, signals.size)
        assertEquals(SignalType.PAUSED, signals.last())
    }

    @Test
    fun `last exercise in group emits GROUP_COMPLETED instead of SET_COMPLETED without double finale`() {
        var nowMs = 0L
        val signals = mutableListOf<SignalType>()
        val engine = PhaseEngine(
            clock = { nowMs },
            onSignal = { sig, _, _ -> signals.add(sig) }
        )

        val groupFinalProto = DemoProtocolConfig(
            id = "group_final",
            titleRu = "Финал группы",
            contractionMs = 2_000L,
            relaxationMs = 2_000L,
            repetitions = 1,
            isLastInGroup = true
        )

        engine.start(groupFinalProto)
        repeat(7) {
            nowMs += 1_000L
            engine.tick()
        }

        assertEquals(RunStatus.COMPLETED, engine.currentSnapshot().status)
        assertEquals(1, signals.count { it == SignalType.GROUP_COMPLETED })
        assertEquals(0, signals.count { it == SignalType.SET_COMPLETED })
    }

    @Test
    fun `checkpoints fire at start, phase change, periodic 5s, pause, resume, and complete`() {
        var nowMs = 0L
        val triggers = mutableListOf<Pair<CheckpointTrigger, PhaseEngineSnapshot>>()
        val engine = PhaseEngine(
            clock = { nowMs },
            onCheckpoint = { snap, trigger -> triggers.add(trigger to snap) }
        )

        val proto = DemoProtocolConfig(
            id = "cp_proto",
            titleRu = "Длинная фаза",
            contractionMs = 6_000L,
            relaxationMs = 2_000L,
            repetitions = 1
        )

        engine.start(protocol = proto, runId = "run_cp_1", slotId = "slot_cp_1", startDate = "2026-11-03")
        assertEquals(CheckpointTrigger.START, triggers.last().first)

        repeat(3) {
            nowMs += 1_000L
            engine.tick()
        }
        assertTrue(triggers.any { it.first == CheckpointTrigger.PHASE_CHANGE })

        repeat(5) {
            nowMs += 1_000L
            engine.tick()
        }
        assertTrue(triggers.any { it.first == CheckpointTrigger.PERIODIC_5S })

        repeat(3) {
            nowMs += 1_000L
            engine.tick()
        }
        assertEquals(CheckpointTrigger.COMPLETE, triggers.last().first)
        assertEquals(8_000L, triggers.last().second.confirmedActiveMs)
    }

    @Test
    fun `started run preserves protocol and sound pack snapshot when external selection changes`() {
        var nowMs = 0L
        val emittedPacks = mutableListOf<SoundPack>()
        val emittedMetronomeStyles = mutableListOf<MetronomeTickStyle>()
        val engine = PhaseEngine(
            clock = { nowMs },
            onSignal = { _, pack, metro ->
                emittedPacks.add(pack)
                emittedMetronomeStyles.add(metro)
            }
        )

        val initialProto = DemoProtocolConfig(
            id = "proto_initial",
            titleRu = "Исходный",
            contractionMs = 2_000L,
            relaxationMs = 2_000L,
            repetitions = 1
        )
        val otherProto = DemoProtocolConfig(
            id = "proto_other",
            titleRu = "Другой",
            contractionMs = 5_000L,
            relaxationMs = 5_000L,
            repetitions = 4
        )

        engine.start(
            protocol = initialProto,
            soundPackSnapshot = SoundPack.CHIMES,
            metronomeStyleSnapshot = MetronomeTickStyle.CRISP
        )

        assertFalse(engine.selectProtocolIfIdle(otherProto))
        assertEquals("proto_initial", engine.currentSnapshot().protocol.id)

        repeat(7) {
            nowMs += 1_000L
            engine.tick()
        }
        assertEquals(RunStatus.COMPLETED, engine.currentSnapshot().status)
        assertTrue(emittedPacks.all { it == SoundPack.CHIMES })
        assertTrue(emittedMetronomeStyles.all { it == MetronomeTickStyle.CRISP })
    }

    @Test
    fun `visual smoothness helpers interpolate continuously without altering phase durations or signal timing`() {
        var nowMs = 1_000L
        val signalTimestamps = mutableListOf<Pair<Long, SignalType>>()
        val engine = PhaseEngine(
            clock = { nowMs },
            onSignal = { sig, _, _ -> signalTimestamps.add(nowMs to sig) }
        )

        val proto = DemoProtocolConfig(
            id = "smooth_test",
            titleRu = "Плавность",
            contractionMs = 2_000L,
            relaxationMs = 2_000L,
            repetitions = 1
        )

        engine.start(proto)
        repeat(3) {
            nowMs += 1_000L
            engine.tick()
        }
        val runningSnap = engine.currentSnapshot()
        assertEquals(RunStatus.RUNNING, runningSnap.status)
        assertEquals(2_000L, runningSnap.phaseRemainingMs)
        assertEquals(4_000L, runningSnap.snapshotElapsedRealtimeMs)

        val p0 = runningSnap.computeVisualRingProgress(4_000L)
        val p1 = runningSnap.computeVisualRingProgress(4_016L)
        val p2 = runningSnap.computeVisualRingProgress(4_250L)
        val p3 = runningSnap.computeVisualRingProgress(4_500L)

        assertEquals(1.0f, p0, 0.0001f)
        assertTrue(p1 < p0 && p2 < p1 && p3 < p2)
        assertEquals(0.75f, p3, 0.001f)
        assertEquals(2L, runningSnap.computeDisplayWholeSeconds(4_500L))
        assertEquals(1L, runningSnap.computeDisplayWholeSeconds(5_100L))

        nowMs = 4_600L
        engine.tick()
        val pausedSnap = engine.pause("Пауза")
        val pausedProgressNow = pausedSnap.computeVisualRingProgress(4_600L)
        val pausedProgressLater = pausedSnap.computeVisualRingProgress(10_000L)
        assertEquals(pausedProgressNow, pausedProgressLater, 0.00001f)
    }

    @Test
    fun `all 3 sound packs have RMS-balanced main commands, quieter metronome, and no volume drop after preparation`() {
        val sampleRate = 24_000
        for (pack in SoundPack.entries) {
            val prepPcm = SoundSynthesisSpec.synthesizePcm16(
                pattern = SoundSynthesisSpec.resolveSignalPattern(pack, SignalType.PREP_TICK),
                volume = 0.8f,
                sampleRate = sampleRate
            )
            val contractionPcm = SoundSynthesisSpec.synthesizePcm16(
                pattern = SoundSynthesisSpec.resolveSignalPattern(pack, SignalType.CONTRACTION),
                volume = 0.8f,
                sampleRate = sampleRate
            )
            val relaxationPcm = SoundSynthesisSpec.synthesizePcm16(
                pattern = SoundSynthesisSpec.resolveSignalPattern(pack, SignalType.RELAXATION),
                volume = 0.8f,
                sampleRate = sampleRate
            )
            val setCompletedPcm = SoundSynthesisSpec.synthesizePcm16(
                pattern = SoundSynthesisSpec.resolveSignalPattern(pack, SignalType.SET_COMPLETED),
                volume = 0.8f,
                sampleRate = sampleRate
            )
            val groupCompletedPcm = SoundSynthesisSpec.synthesizePcm16(
                pattern = SoundSynthesisSpec.resolveSignalPattern(pack, SignalType.GROUP_COMPLETED),
                volume = 0.8f,
                sampleRate = sampleRate
            )

            val prepRms = SoundSynthesisSpec.computeActiveRms(prepPcm)
            val contractionRms = SoundSynthesisSpec.computeActiveRms(contractionPcm)
            val relaxationRms = SoundSynthesisSpec.computeActiveRms(relaxationPcm)
            val setCompletedRms = SoundSynthesisSpec.computeActiveRms(setCompletedPcm)
            val groupCompletedRms = SoundSynthesisSpec.computeActiveRms(groupCompletedPcm)

            assertTrue(
                "RMS сокращения ($contractionRms) и расслабления ($relaxationRms) в наборе ${pack.id} должны быть сопоставимы",
                abs(contractionRms - relaxationRms) < 0.015
            )
            assertTrue(
                "RMS сокращения ($contractionRms) и финала подхода ($setCompletedRms) должны быть сопоставимы",
                abs(contractionRms - setCompletedRms) < 0.015
            )
            assertTrue(
                "RMS сокращения ($contractionRms) и финала группы ($groupCompletedRms) должны быть сопоставимы",
                abs(contractionRms - groupCompletedRms) < 0.015
            )
            assertTrue(
                "PREP_TICK ($prepRms) не должен быть громче CONTRACTION ($contractionRms)",
                prepRms <= contractionRms
            )

            for (pcm in listOf(prepPcm, contractionPcm, relaxationPcm, setCompletedPcm, groupCompletedPcm)) {
                assertTrue(pcm.isNotEmpty())
                val durationMs = (pcm.size * 1000L) / sampleRate
                assertTrue("Длительность $durationMs мс должна быть <= 360 мс", durationMs <= 360L)
                assertEquals(0.toShort(), pcm.first())
                assertEquals(0.toShort(), pcm.last())
            }
        }

        val commandVol = SoundSynthesisSpec.computeEffectiveVolume(
            signal = SignalType.CONTRACTION,
            signalVolume = 0.80f,
            metronomeVolume = 1.00f
        )
        val metroVol = SoundSynthesisSpec.computeEffectiveVolume(
            signal = SignalType.METRONOME_TICK,
            signalVolume = 0.80f,
            metronomeVolume = 1.00f
        )
        assertTrue("Метроном ($metroVol) должен быть тише команды ($commandVol)", metroVol < commandVol)
    }

    @Test
    fun `markdown novel parser parses 3 acts and 12 chapters, Cyrillic, long paragraphs, dividers, and preserves exact author text in both modes`() {
        val rawMarkdown = NeutralTestMarkdownFactory.buildThreeActsTwelveChaptersMarkdown()
        val rawBytes = rawMarkdown.toByteArray(Charsets.UTF_8)

        val parsed = MarkdownNovelParser.parseMarkdownNovel(
            rawBytes = rawBytes,
            fileName = "neutral_test_3acts_12chapters.md",
            headingRule = HeadingLevelRule()
        ).getOrThrow()

        // 1. Разбор заголовков и 12 глав в 3 актах
        assertEquals("Нейтральный технический макет (3 акта, 12 глав)", parsed.explicitBookTitle)
        assertEquals(3, parsed.acts.size)
        assertEquals(12, parsed.chapters.size)
        assertEquals(listOf(4, 4, 4), parsed.acts.map { it.chapters.size })

        // 2. Кириллица, пустые строки, длинные абзацы, цитаты, выделения и разделители
        for ((index, chapter) in parsed.chapters.withIndex()) {
            val chapterNum = index + 1
            assertEquals(String.format(java.util.Locale.US, "ch_%02d", chapterNum), chapter.chapterId)
            assertTrue(chapter.title.contains("Глава $chapterNum"))
            assertEquals(5, chapter.blocks.size)

            val p1 = chapter.blocks[0]
            val p2Long = chapter.blocks[1]
            val quote = chapter.blocks[2]
            val divider = chapter.blocks[3]
            val p3Final = chapter.blocks[4]

            assertEquals(NovelBlockType.PARAGRAPH, p1.type)
            assertTrue(p1.plainText.contains("Абзац $chapterNum.1. Контрольный кириллический текст главы $chapterNum"))
            assertTrue(p1.inlineSpans.any { it.style == NovelInlineStyle.ITALIC })
            assertTrue(p1.inlineSpans.any { it.style == NovelInlineStyle.BOLD })

            assertEquals(NovelBlockType.PARAGRAPH, p2Long.type)
            assertTrue("Длинный абзац должен сохраняться целиком без разрывов", p2Long.plainText.length > 200)
            assertTrue(p2Long.plainText.contains("Абзац $chapterNum.2 (длинный абзац для проверки прокрутки и переноса строк)"))

            assertEquals(NovelBlockType.BLOCKQUOTE, quote.type)
            assertTrue(quote.plainText.contains("Цитата $chapterNum.3"))

            assertEquals(NovelBlockType.DIVIDER, divider.type)

            assertEquals(NovelBlockType.PARAGRAPH, p3Final.type)
            assertTrue(p3Final.plainText.contains("Абзац $chapterNum.4 после горизонтального разделителя сцены."))
            assertTrue(p3Final.plainText.contains("CH_${chapterNum}_END"))

            // 3. Отсутствие пропусков, дублирования и перестановки текста
            val blockIndices = chapter.blocks.map { it.orderInChapter }
            assertEquals(listOf(0, 1, 2, 3, 4), blockIndices)

            // 4. Одинаковый авторский текст в обоих режимах отображения («Книга» и «Пошаговый»)
            val bookModeBlocks = chapter.blocks
            val stepModeBlocks = chapter.steps.flatMap { it.blocks }
            assertEquals(
                "Число блоков в режиме «Книга» и суммарно в шагах режима «Пошаговый» должно совпадать",
                bookModeBlocks.size,
                stepModeBlocks.size
            )
            assertEquals(
                "Идентификаторы и порядок блоков в обоих режимах обязаны совпадать 1-в-1",
                bookModeBlocks.map { it.blockId },
                stepModeBlocks.map { it.blockId }
            )
            assertEquals(
                "Отображаемый авторский текст в обоих режимах обязан совпадать символ-в-символ",
                bookModeBlocks.map { it.plainText },
                stepModeBlocks.map { it.plainText }
            )
        }
    }

    @Test
    fun `markdown novel parser rejects corrupted binary or empty files with clear Russian error`() {
        val invalidUtf8 = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00.toByte(), 0x81.toByte())
        val binaryResult = MarkdownNovelParser.parseMarkdownNovel(invalidUtf8, "broken.md")
        assertTrue(binaryResult.isFailure)

        val emptyResult = MarkdownNovelParser.parseMarkdownNovel("   \n\n  ".toByteArray(Charsets.UTF_8), "empty.md")
        assertTrue(emptyResult.isFailure)
    }

    @Test
    fun `calculateFreeCycles exact cycles division`() {
        // Выбрано 5:00 = 300 с. Сокращение 20 с, отдых 10 с. Цикл = 30 с.
        // Полных циклов: 10. Фактическое время: 300 с (5:00). Остаток: 0 с.
        val calc = calculateFreeCycles(desiredDurationSec = 300, contractionSec = 20, relaxationSec = 10)
        assertTrue(calc.isValid)
        assertEquals(30, calc.cycleSec)
        assertEquals(10, calc.completedCycles)
        assertEquals(300, calc.actualDurationSec)
        assertEquals(0, calc.unusedRemainderSec)
        assertEquals("Выбрано 5:00. 10 полных циклов, ровно 5:00", calc.summaryRu)
    }

    @Test
    fun `calculateFreeCycles non-exact cycles division`() {
        // Выбрано 3:00 = 180 с. Сокращение 20 с, отдых 15 с. Цикл = 35 с.
        // Полных циклов: 5 (175 с = 2:55). Остаток: 5 с.
        val calc = calculateFreeCycles(desiredDurationSec = 180, contractionSec = 20, relaxationSec = 15)
        assertTrue(calc.isValid)
        assertEquals(35, calc.cycleSec)
        assertEquals(5, calc.completedCycles)
        assertEquals(175, calc.actualDurationSec)
        assertEquals(5, calc.unusedRemainderSec)
        assertEquals("Выбрано 3:00. Получится 5 полных циклов, 2:55", calc.summaryRu)
    }

    @Test
    fun `calculateFreeCycles duration too short for even one cycle returns invalid with clear explanation`() {
        // Выбрано 30 с. Сокращение 20 с, отдых 15 с. Цикл = 35 с.
        // 0 полных циклов.
        val calc = calculateFreeCycles(desiredDurationSec = 30, contractionSec = 20, relaxationSec = 15)
        assertFalse(calc.isValid)
        assertEquals(0, calc.completedCycles)
        assertEquals(0, calc.actualDurationSec)
        assertEquals("Длительность меньше одного цикла. Увеличьте общее время или уменьшите интервалы.", calc.summaryRu)
    }

    @Test
    fun `calculateFreeCycles technical limits verification up to 90 minutes and 300 cycles`() {
        // 90 минут = 5400 с. 1 с удержание + 1 с отдых = 2 с цикл.
        // 5400 / 2 = 2700 циклов > 300 -> не меняем введённые значения молча, возвращаем понятное объяснение (v1.4 п. 5)
        val calcOver300 = calculateFreeCycles(desiredDurationSec = 5400, contractionSec = 1, relaxationSec = 1)
        assertFalse(calcOver300.isValid)
        assertEquals(2700, calcOver300.completedCycles)
        assertTrue(calcOver300.summaryRu.contains("Превышен предел 300 циклов"))

        // Ровно 300 циклов (10 минут по 1 с + 1 с) -> допустимо
        val calcExact300 = calculateFreeCycles(desiredDurationSec = 600, contractionSec = 1, relaxationSec = 1)
        assertTrue(calcExact300.isValid)
        assertEquals(300, calcExact300.completedCycles)
        assertEquals(600, calcExact300.actualDurationSec)

        // 120 с удержание + 120 с отдых = 240 с цикл.
        // 30 минут = 1800 с -> 7 циклов (1680 с = 28:00)
        val calcLargeIntervals = calculateFreeCycles(desiredDurationSec = 1800, contractionSec = 120, relaxationSec = 120)
        assertTrue(calcLargeIntervals.isValid)
        assertEquals(240, calcLargeIntervals.cycleSec)
        assertEquals(7, calcLargeIntervals.completedCycles)
        assertEquals(1680, calcLargeIntervals.actualDurationSec)
        assertEquals("Выбрано 30:00. Получится 7 полных циклов, 28:00", calcLargeIntervals.summaryRu)
    }

    @Test
    fun `free mode run preserves snapshot parameters when template is modified after launch`() {
        var nowMs = 0L
        val engine = PhaseEngine(clock = { nowMs })

        val templateProto = DemoProtocolConfig(
            id = "free_proto_snap",
            titleRu = "Мои интервалы · Быстрый",
            contractionMs = 5_000L,
            relaxationMs = 5_000L,
            repetitions = 3
        )

        assertTrue(engine.start(protocol = templateProto, isFreeMode = true))
        val activeSnap = engine.currentSnapshot()
        assertTrue(activeSnap.isFreeMode)
        assertEquals("free_proto_snap", activeSnap.protocol.id)
        assertEquals(5_000L, activeSnap.protocol.contractionMs)
        assertEquals("Удержание", PhaseType.CONTRACTION.labelRu)
        assertEquals("Отдых", PhaseType.RELAXATION.labelRu)

        // Попытка запустить вторую тренировку или изменить протокол во время работы должна быть отклонена
        val modifiedProto = DemoProtocolConfig(
            id = "free_proto_snap",
            titleRu = "Мои интервалы · Изменённый",
            contractionMs = 20_000L,
            relaxationMs = 20_000L,
            repetitions = 10
        )
        assertFalse(engine.start(protocol = modifiedProto, isFreeMode = true))
        assertFalse(engine.selectProtocolIfIdle(modifiedProto))

        // Протокол в активной попытке остаётся исходным
        assertEquals(5_000L, engine.currentSnapshot().protocol.contractionMs)
        assertEquals(3, engine.currentSnapshot().protocol.repetitions)
    }
}

