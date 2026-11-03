package com.example

import com.example.domain.DemoProtocolConfig
import com.example.domain.PhaseEngine
import com.example.domain.PhaseType
import com.example.domain.RunStatus
import com.example.domain.SignalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `rapid duplicate start is blocked while session is active`() {
        var nowMs = 1_000L
        val signals = mutableListOf<SignalType>()
        val engine = PhaseEngine(
            clock = { nowMs },
            onSignal = { signals.add(it) }
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
            onSignal = { signals.add(it) }
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
            onSignal = { signals.add(it) }
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
            onSignal = { signals.add(it) }
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
            onSignal = { signals.add(it) }
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
}
