package com.example

import com.example.domain.*
import org.junit.Assert.*
import org.junit.Test

class WorkoutReliabilityTest {
    private class Clock : MonotonicClock {
        var now = 0L
        override fun elapsedRealtimeMs() = now
    }
    private val clock = Clock()
    private val signals = mutableListOf<SignalType>()
    private val engine = PhaseEngine(clock, onSignal = { signal, _, _ -> signals += signal })
    private val protocol = DemoProtocolConfig("test", "Тест", 1000, 1000, 2, true, PlanGroup.MORNING)
    private fun advance(ms: Long) { repeat((ms / 50).toInt()) { clock.now += 50; engine.tick() } }
    private fun custom() = CustomWorkoutProgram(title = "Программа", blocks = listOf(
        WorkoutBlock(contractionSec = 1, relaxationSec = 1, repetitions = 1)))

    @Test fun normalWorkoutAfterCustomUsesItsOwnRepetitions() {
        engine.startCustomProgram(custom())
        advance(5000)
        assertEquals(RunStatus.COMPLETED, engine.currentSnapshot().status)
        engine.start(protocol)
        advance(5000)
        assertEquals(RunStatus.RUNNING, engine.currentSnapshot().status)
        assertEquals(2, engine.currentSnapshot().currentRepetition)
        advance(2000)
        assertEquals(RunStatus.COMPLETED, engine.currentSnapshot().status)
        assertEquals(4000L, engine.currentSnapshot().confirmedActiveMs)
    }
    @Test fun pauseDoesNotCountWaitingOrResumePreparation() {
        engine.start(protocol)
        advance(3500)
        engine.pause()
        clock.now += 100000
        engine.tick()
        assertEquals(500L, engine.currentSnapshot().confirmedActiveMs)
        assertTrue(engine.resume())
        advance(3000)
        assertEquals(500L, engine.currentSnapshot().confirmedActiveMs)
        advance(500)
        assertEquals(PhaseType.RELAXATION, engine.currentSnapshot().currentPhase)
    }
    @Test fun schedulerGapInterruptsWithoutAwardingMissedTime() {
        engine.start(protocol)
        advance(3500)
        signals.clear()
        clock.now += 10000
        engine.tick()
        assertEquals(RunStatus.INTERRUPTED, engine.currentSnapshot().status)
        assertEquals(500L, engine.currentSnapshot().confirmedActiveMs)
        assertEquals(listOf(SignalType.PAUSED), signals)
    }
    @Test fun completionHappensOnceAndRejectsStaleStopCommand() {
        engine.start(protocol)
        advance(7000)
        val completed = engine.currentSnapshot()
        engine.confirmStop()
        advance(5000)
        assertEquals(completed, engine.currentSnapshot())
        assertEquals(1, signals.count { it == SignalType.GROUP_COMPLETED })
    }
    @Test fun rapidSecondStartAndCancelledStopDoNotRestartWorkout() {
        engine.start(protocol)
        assertFalse(engine.start(protocol))
        advance(3500)
        engine.requestStop()
        engine.cancelStopDialog()
        assertEquals(RunStatus.PAUSED, engine.currentSnapshot().status)
        engine.confirmStop()
        assertEquals(RunStatus.PAUSED, engine.currentSnapshot().status)
    }
}
