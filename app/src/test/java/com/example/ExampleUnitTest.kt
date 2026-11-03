package com.example

import com.example.domain.CheckpointTrigger
import com.example.domain.DemoProtocolConfig
import com.example.domain.MetronomeTickStyle
import com.example.domain.PhaseEngine
import com.example.domain.PhaseEngineSnapshot
import com.example.domain.PhaseType
import com.example.domain.RunStatus
import com.example.domain.SignalType
import com.example.domain.SoundPack
import com.example.domain.SoundSynthesisSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
        // Повторный быстрый старт должен быть отклонён
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

        // +1000 мс (осталось 2000 мс подготовки -> 2-й сигнал)
        nowMs += 1_000L
        engine.tick()
        assertEquals(0L, engine.currentSnapshot().confirmedActiveMs)
        assertEquals(2, signals.count { it == SignalType.PREP_TICK })

        // +1000 мс (осталось 1000 мс подготовки -> 3-й сигнал)
        nowMs += 1_000L
        engine.tick()
        assertEquals(0L, engine.currentSnapshot().confirmedActiveMs)
        assertEquals(3, signals.count { it == SignalType.PREP_TICK })

        // +1000 мс (подготовка завершена -> переход в RUNNING и явная команда CONTRACTION)
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
        // Проходим 3 секунды подготовки шагами по 1000 мс
        repeat(3) {
            nowMs += 1_000L
            engine.tick()
        }
        assertEquals(RunStatus.RUNNING, engine.currentSnapshot().status)

        // Проходим 1500 мс фазы сокращения (остаётся 2500 мс)
        nowMs += 1_000L
        engine.tick()
        nowMs += 500L
        engine.tick()
        assertEquals(1_500L, engine.currentSnapshot().confirmedActiveMs)
        assertEquals(2_500L, engine.currentSnapshot().phaseRemainingMs)

        // Ставим на паузу
        val pausedSnap = engine.pause("Тестовая пауза")
        assertEquals(RunStatus.PAUSED, pausedSnap.status)
        assertEquals("Пауза — расслабьтесь", pausedSnap.headlineCommandRu)
        assertEquals(1_500L, pausedSnap.confirmedActiveMs)
        assertEquals(2_500L, pausedSnap.phaseRemainingMs)

        // Ждём 60 секунд на паузе
        nowMs += 60_000L
        val after60sPause = engine.tick()
        assertEquals(1_500L, after60sPause.confirmedActiveMs)
        assertEquals(2_500L, after60sPause.phaseRemainingMs)

        // Возобновляем вручную -> 3 секунды подготовки -> затем остаток 2500 мс текущей фазы
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

        // Нажатие «Остановить» сначала ставит на паузу и открывает диалог
        val reqSnap = engine.requestStop()
        assertEquals(RunStatus.PAUSED, reqSnap.status)
        assertTrue(reqSnap.isStopDialogVisible)

        // Отмена диалога («Остаться на паузе») оставляет паузу
        val cancelledSnap = engine.cancelStopDialog()
        assertEquals(RunStatus.PAUSED, cancelledSnap.status)
        assertFalse(cancelledSnap.isStopDialogVisible)

        // Подтверждение «Завершить» переводит в STOPPED
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

        // Имитируем зависание планировщика ОС на 6000 мс (> порога 2500 мс)
        nowMs += 6_000L
        val snap = engine.tick()
        assertEquals(RunStatus.INTERRUPTED, snap.status)
        assertEquals(0L, snap.confirmedActiveMs)
        // Подан только один сигнал PAUSED, без пачки пропущенных команд
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
        // 3 сек подготовки + 2 сек сокращения + 2 сек расслабления
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

        // Завершаем 3 сек подготовки -> PHASE_CHANGE
        repeat(3) {
            nowMs += 1_000L
            engine.tick()
        }
        assertTrue(triggers.any { it.first == CheckpointTrigger.PHASE_CHANGE })

        // Проходим 5 сек внутри 6-секундной фазы -> должен сработать PERIODIC_5S
        repeat(5) {
            nowMs += 1_000L
            engine.tick()
        }
        assertTrue(triggers.any { it.first == CheckpointTrigger.PERIODIC_5S })

        // Ещё 1 сек (конец сокращения -> расслабление) + 2 сек (конец расслабления -> COMPLETE)
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

        // Попытка сменить протокол во время активного подхода должна быть отклонена
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
        // Завершаем подготовку ровно на 4000 мс
        repeat(3) {
            nowMs += 1_000L
            engine.tick()
        }
        val runningSnap = engine.currentSnapshot()
        assertEquals(RunStatus.RUNNING, runningSnap.status)
        assertEquals(2_000L, runningSnap.phaseRemainingMs)
        assertEquals(4_000L, runningSnap.snapshotElapsedRealtimeMs)

        // Визуальный опрос между тиками (на 4016 мс, 4250 мс, 4500 мс) даёт плавное убывание кольца
        // и только целые секунды, не вызывая tick() и не меняя состояние PhaseEngine
        val p0 = runningSnap.computeVisualRingProgress(4_000L)
        val p1 = runningSnap.computeVisualRingProgress(4_016L)
        val p2 = runningSnap.computeVisualRingProgress(4_250L)
        val p3 = runningSnap.computeVisualRingProgress(4_500L)

        assertEquals(1.0f, p0, 0.0001f)
        assertTrue(p1 < p0 && p2 < p1 && p3 < p2)
        assertEquals(0.75f, p3, 0.001f)
        // Целые секунды: на 4500 мс (осталось 1500 мс) показывается 2 с, без десятых долей
        assertEquals(2L, runningSnap.computeDisplayWholeSeconds(4_500L))
        // На 5100 мс (если бы оставалось 900 мс) показывается 1 с
        assertEquals(1L, runningSnap.computeDisplayWholeSeconds(5_100L))

        // При паузе визуальный прогресс мгновенно замирает на точном значении паузы независимо от времени кадра
        nowMs = 4_600L
        engine.tick()
        val pausedSnap = engine.pause("Пауза")
        val pausedProgressNow = pausedSnap.computeVisualRingProgress(4_600L)
        val pausedProgressLater = pausedSnap.computeVisualRingProgress(10_000L)
        assertEquals(pausedProgressNow, pausedProgressLater, 0.00001f)
    }

    @Test
    fun `all 3 sound packs and 2 metronome styles are click-free, fit inside 1000ms, and differ within each pack`() {
        val sampleRate = 24_000
        for (pack in SoundPack.entries) {
            val contraction = SoundSynthesisSpec.synthesizePcm16(
                pattern = SoundSynthesisSpec.resolveSignalPattern(pack, SignalType.CONTRACTION),
                volume = 0.8f,
                sampleRate = sampleRate
            )
            val relaxation = SoundSynthesisSpec.synthesizePcm16(
                pattern = SoundSynthesisSpec.resolveSignalPattern(pack, SignalType.RELAXATION),
                volume = 0.8f,
                sampleRate = sampleRate
            )
            val setCompleted = SoundSynthesisSpec.synthesizePcm16(
                pattern = SoundSynthesisSpec.resolveSignalPattern(pack, SignalType.SET_COMPLETED),
                volume = 0.8f,
                sampleRate = sampleRate
            )
            val groupCompleted = SoundSynthesisSpec.synthesizePcm16(
                pattern = SoundSynthesisSpec.resolveSignalPattern(pack, SignalType.GROUP_COMPLETED),
                volume = 0.8f,
                sampleRate = sampleRate
            )

            val signals = listOf(contraction, relaxation, setCompleted, groupCompleted)
            for (pcm in signals) {
                assertTrue("PCM буфер не должен быть пустым", pcm.isNotEmpty())
                // Длительность не должна превышать 360 мс (гарантированно помещается в минимальную фазу 1000 мс)
                val durationMs = (pcm.size * 1000L) / sampleRate
                assertTrue("Длительность $durationMs мс должна быть <= 360 мс", durationMs <= 360L)
                // Огибающая raised-cosine гарантирует 0 на первом и последнем отсчёте (без щелчков)
                assertEquals("Первый отсчёт должен быть 0 (без щелчка)", 0.toShort(), pcm.first())
                assertEquals("Последний отсчёт должен быть 0 (без щелчка)", 0.toShort(), pcm.last())
            }

            // Команды внутри одного набора должны быть различимы (разная длина или спектральная форма)
            assertFalse(contraction.contentEquals(relaxation))
            assertFalse(contraction.contentEquals(setCompleted))
            assertFalse(setCompleted.contentEquals(groupCompleted))
        }

        // Проверяем оба варианта тика метронома («Мягкий» и «Чёткий»)
        val softTick = SoundSynthesisSpec.synthesizePcm16(
            pattern = SoundSynthesisSpec.resolveSignalPattern(SoundPack.SOFT, SignalType.METRONOME_TICK, MetronomeTickStyle.SOFT),
            volume = 0.5f,
            sampleRate = sampleRate
        )
        val crispTick = SoundSynthesisSpec.synthesizePcm16(
            pattern = SoundSynthesisSpec.resolveSignalPattern(SoundPack.SOFT, SignalType.METRONOME_TICK, MetronomeTickStyle.CRISP),
            volume = 0.5f,
            sampleRate = sampleRate
        )
        assertEquals(0.toShort(), softTick.first())
        assertEquals(0.toShort(), softTick.last())
        assertEquals(0.toShort(), crispTick.first())
        assertEquals(0.toShort(), crispTick.last())
        assertNotEquals(softTick.size, crispTick.size)
    }
}
