package com.example
import com.example.domain.ChantalJourneyClock as Clock
import org.junit.Test
import org.junit.Assert.*
class WorkoutJourneyClockTest {
    @Test fun completionRequiresFullHourAndValidStart() {
        assertFalse(Clock.ready(0, Long.MAX_VALUE))
        assertFalse(Clock.ready(100, 99))
        assertFalse(Clock.ready(100, 100 + Clock.DURATION_MS - 1))
        assertTrue(Clock.ready(100, 100 + Clock.DURATION_MS))
        assertEquals(0L, Clock.remainingMs(100, 100 + Clock.DURATION_MS + 1000))
    }
}
