package com.saferouteai.domain.time

import com.saferouteai.domain.model.session.CheckpointState
import com.saferouteai.domain.model.session.JourneySession
import com.saferouteai.domain.model.session.SessionStatus
import com.saferouteai.test.FakeClock
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockTest {

    @Test
    fun `fake clock advances time deterministically without sleeping`() {
        val clock = FakeClock(1_000_000L)
        assertEquals(1_000_000L, clock.nowEpochMs())

        clock.advanceTimeBy(60_000L) // 1 minute
        assertEquals(1_060_000L, clock.nowEpochMs())

        clock.advanceTimeBy(900_000L) // 15 minutes
        assertEquals(1_960_000L, clock.nowEpochMs())
    }

    @Test
    fun `journey session calculates active duration from clock`() {
        val clock = FakeClock(1_000_000L)
        val session = JourneySession(
            sessionId = "test-session",
            status = SessionStatus.ACTIVE,
            startedAtEpochMs = 1_000_000L,
            endedAtEpochMs = null,
            checkpointState = CheckpointState.SCHEDULED
        )

        assertEquals(0L, session.calculateDurationMs(clock))

        clock.advanceTimeBy(300_000L) // 5 minutes later
        assertEquals(300_000L, session.calculateDurationMs(clock))
    }

    @Test
    fun `journey session calculates completed duration from ended timestamp`() {
        val clock = FakeClock(2_000_000L)
        val session = JourneySession(
            sessionId = "test-session",
            status = SessionStatus.COMPLETED,
            startedAtEpochMs = 1_000_000L,
            endedAtEpochMs = 1_450_000L,
            checkpointState = CheckpointState.DISARMED
        )

        // Duration is fixed at 450_000L regardless of current clock time
        assertEquals(450_000L, session.calculateDurationMs(clock))

        clock.advanceTimeBy(500_000L)
        assertEquals(450_000L, session.calculateDurationMs(clock))
    }
}
