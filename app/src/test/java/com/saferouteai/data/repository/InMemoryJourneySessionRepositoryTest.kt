package com.saferouteai.data.repository

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.location.UserLocation
import com.saferouteai.domain.model.session.CheckpointPolicy
import com.saferouteai.domain.model.session.CheckpointState
import com.saferouteai.domain.model.session.SessionStatus
import com.saferouteai.test.FakeClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InMemoryJourneySessionRepositoryTest {

    private lateinit var clock: FakeClock
    private lateinit var repository: InMemoryJourneySessionRepository

    @Before
    fun setUp() {
        clock = FakeClock(1_000_000L)
        repository = InMemoryJourneySessionRepository(clock)
    }

    @Test
    fun `initial status is IDLE and activeSession is null`() {
        assertEquals(SessionStatus.IDLE, repository.sessionStatus.value)
        assertNull(repository.activeSession.value)
    }

    @Test
    fun `startSession transitions to ACTIVE and schedules checkpoint`() = runTest {
        val policy = CheckpointPolicy(
            enabled = true,
            intervalMs = 600_000L, // 10 mins
            acknowledgementWindowMs = 120_000L // 2 mins
        )

        val result = repository.startSession("Home", "Work", policy)
        assertTrue(result is Result.Success)
        val session = (result as Result.Success).data

        assertEquals(SessionStatus.ACTIVE, repository.sessionStatus.value)
        assertEquals("Home", session.origin)
        assertEquals("Work", session.destination)
        assertEquals(1_000_000L, session.startedAtEpochMs)
        assertEquals(1_600_000L, session.nextCheckpointEpochMs)
        assertEquals(CheckpointState.SCHEDULED, session.checkpointState)
    }

    @Test
    fun `cannot start session when already active`() = runTest {
        repository.startSession()
        val secondStart = repository.startSession()
        assertTrue(secondStart is Result.Error)
    }

    @Test
    fun `triggerCheckpointDue moves status to CHECKPOINT_DUE`() = runTest {
        repository.startSession()
        clock.advanceTimeBy(600_000L)

        val result = repository.triggerCheckpointDue()
        assertTrue(result is Result.Success)
        assertEquals(SessionStatus.CHECKPOINT_DUE, repository.sessionStatus.value)
        assertEquals(CheckpointState.DUE, repository.activeSession.value?.checkpointState)
    }

    @Test
    fun `acknowledgeCheckpoint moves status back to ACTIVE and schedules next checkpoint`() = runTest {
        val policy = CheckpointPolicy(intervalMs = 600_000L)
        repository.startSession(policy = policy)

        clock.advanceTimeBy(600_000L)
        repository.triggerCheckpointDue()

        // User acknowledges at t = 1_605_000L
        clock.advanceTimeBy(5_000L)
        val ackResult = repository.acknowledgeCheckpoint()
        assertTrue(ackResult is Result.Success)

        val updated = repository.activeSession.value
        assertNotNull(updated)
        assertEquals(SessionStatus.ACTIVE, repository.sessionStatus.value)
        assertEquals(1_605_000L, updated?.lastCheckpointEpochMs)
        assertEquals(1_605_000L + 600_000L, updated?.nextCheckpointEpochMs)
        assertEquals(CheckpointState.SCHEDULED, updated?.checkpointState)
    }

    @Test
    fun `checkCheckpointExpiry marks checkpoint as MISSED when window expires`() = runTest {
        val policy = CheckpointPolicy(
            intervalMs = 600_000L, // next at 1_600_000L
            acknowledgementWindowMs = 120_000L // expires at 1_720_000L
        )
        repository.startSession(policy = policy)

        clock.advanceTimeBy(600_000L)
        repository.triggerCheckpointDue()

        // Within window: not expired
        clock.advanceTimeBy(60_000L) // t = 1_660_000L
        repository.checkCheckpointExpiry()
        assertEquals(CheckpointState.DUE, repository.activeSession.value?.checkpointState)

        // Beyond window: expired
        clock.advanceTimeBy(70_000L) // t = 1_730_000L (> 1_720_000L)
        repository.checkCheckpointExpiry()
        assertEquals(CheckpointState.MISSED, repository.activeSession.value?.checkpointState)
    }

    @Test
    fun `endSession moves status to COMPLETED and clears checkpoints`() = runTest {
        repository.startSession()
        clock.advanceTimeBy(300_000L)

        val result = repository.endSession()
        assertTrue(result is Result.Success)
        val completed = (result as Result.Success).data

        assertEquals(SessionStatus.COMPLETED, repository.sessionStatus.value)
        assertEquals(1_300_000L, completed.endedAtEpochMs)
        assertEquals(CheckpointState.DISARMED, completed.checkpointState)
    }

    @Test
    fun `cancelSession moves status to CANCELLED`() = runTest {
        repository.startSession()
        clock.advanceTimeBy(100_000L)

        val result = repository.cancelSession()
        assertTrue(result is Result.Success)
        val cancelled = (result as Result.Success).data

        assertEquals(SessionStatus.CANCELLED, repository.sessionStatus.value)
        assertEquals(1_100_000L, cancelled.endedAtEpochMs)
    }

    @Test
    fun `resetToIdle resets state from COMPLETED`() = runTest {
        repository.startSession()
        repository.endSession()
        val resetResult = repository.resetToIdle()
        assertTrue(resetResult is Result.Success)
        assertEquals(SessionStatus.IDLE, repository.sessionStatus.value)
        assertNull(repository.activeSession.value)
    }

    @Test
    fun `updateLocation modifies active session volatile coordinate`() = runTest {
        repository.startSession()
        val location = UserLocation(
            latitude = 37.7749,
            longitude = -122.4194,
            altitudeMeters = 10.0,
            accuracyMeters = 5.0f,
            speedMps = 0f,
            bearingDegrees = 0f,
            timestampEpochMs = clock.nowEpochMs()
        )

        repository.updateLocation(location)
        assertEquals(location, repository.activeSession.value?.latestLocation)
    }
}
