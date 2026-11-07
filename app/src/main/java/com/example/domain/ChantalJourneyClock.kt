package com.example.domain

object ChantalJourneyClock {
    const val DURATION_MS = 60 * 60 * 1000L
    fun remainingMs(start: Long, now: Long): Long = if (start <= 0) DURATION_MS else
        (DURATION_MS - (now - start).coerceAtLeast(0)).coerceAtLeast(0)
    fun ready(start: Long, now: Long): Boolean = start > 0 && remainingMs(start, now) == 0L
}
