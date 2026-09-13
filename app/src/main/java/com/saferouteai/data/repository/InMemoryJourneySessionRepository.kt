package com.saferouteai.data.repository

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.location.UserLocation
import com.saferouteai.domain.model.session.CheckpointPolicy
import com.saferouteai.domain.model.session.CheckpointState
import com.saferouteai.domain.model.session.JourneySession
import com.saferouteai.domain.model.session.SessionStatus
import com.saferouteai.domain.repository.JourneySessionRepository
import com.saferouteai.domain.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Pure in-memory, thread-safe test double and implementation of [JourneySessionRepository].
 * Operates deterministically via an injected [Clock] with zero Android dependencies.
 */
class InMemoryJourneySessionRepository(
    private val clock: Clock
) : JourneySessionRepository {

    private val mutex = Mutex()

    private val _activeSession = MutableStateFlow<JourneySession?>(null)
    override val activeSession: StateFlow<JourneySession?> = _activeSession.asStateFlow()

    private val _sessionStatus = MutableStateFlow(SessionStatus.IDLE)
    override val sessionStatus: StateFlow<SessionStatus> = _sessionStatus.asStateFlow()

    private var activePolicy: CheckpointPolicy = CheckpointPolicy()

    override suspend fun startSession(
        origin: String?,
        destination: String?,
        policy: CheckpointPolicy
    ): Result<JourneySession> = mutex.withLock {
        val current = _sessionStatus.value
        if (!current.canTransitionTo(SessionStatus.STARTING)) {
            return Result.Error(IllegalStateException("Cannot start session from status: $current"))
        }

        activePolicy = policy
        val now = clock.nowEpochMs()
        val nextCheckpoint = if (policy.enabled) now + policy.intervalMs else null
        val initialCheckpointState = if (policy.enabled) CheckpointState.SCHEDULED else CheckpointState.DISARMED

        val session = JourneySession(
            sessionId = UUID.randomUUID().toString(),
            status = SessionStatus.ACTIVE,
            startedAtEpochMs = now,
            endedAtEpochMs = null,
            origin = origin,
            destination = destination,
            latestLocation = null,
            lastCheckpointEpochMs = null,
            nextCheckpointEpochMs = nextCheckpoint,
            checkpointState = initialCheckpointState
        )

        _activeSession.value = session
        _sessionStatus.value = SessionStatus.ACTIVE
        Result.Success(session)
    }

    override suspend fun acknowledgeCheckpoint(): Result<JourneySession> = mutex.withLock {
        val current = _sessionStatus.value
        if (!current.canTransitionTo(SessionStatus.CHECKPOINT_ACKNOWLEDGED)) {
            return Result.Error(IllegalStateException("Cannot acknowledge checkpoint from status: $current"))
        }

        val session = _activeSession.value ?: return Result.Error(IllegalStateException("No active session found"))
        val now = clock.nowEpochMs()
        val nextCheckpoint = if (activePolicy.enabled) now + activePolicy.intervalMs else null

        val acknowledged = session.copy(
            status = SessionStatus.ACTIVE,
            lastCheckpointEpochMs = now,
            nextCheckpointEpochMs = nextCheckpoint,
            checkpointState = if (activePolicy.enabled) CheckpointState.SCHEDULED else CheckpointState.DISARMED
        )

        _activeSession.value = acknowledged
        _sessionStatus.value = SessionStatus.ACTIVE
        Result.Success(acknowledged)
    }

    override suspend fun triggerCheckpointDue(): Result<JourneySession> = mutex.withLock {
        val current = _sessionStatus.value
        if (!current.canTransitionTo(SessionStatus.CHECKPOINT_DUE)) {
            return Result.Error(IllegalStateException("Cannot trigger checkpoint due from status: $current"))
        }

        val session = _activeSession.value ?: return Result.Error(IllegalStateException("No active session found"))
        val updated = session.copy(
            status = SessionStatus.CHECKPOINT_DUE,
            checkpointState = CheckpointState.DUE
        )

        _activeSession.value = updated
        _sessionStatus.value = SessionStatus.CHECKPOINT_DUE
        Result.Success(updated)
    }

    override suspend fun checkCheckpointExpiry(): Result<Unit> = mutex.withLock {
        val session = _activeSession.value ?: return Result.Success(Unit)
        if (session.status != SessionStatus.CHECKPOINT_DUE) return Result.Success(Unit)

        val nextCheckpoint = session.nextCheckpointEpochMs ?: return Result.Success(Unit)
        val expiryTime = nextCheckpoint + activePolicy.acknowledgementWindowMs
        val now = clock.nowEpochMs()

        if (now > expiryTime) {
            val missed = session.copy(
                checkpointState = CheckpointState.MISSED
            )
            _activeSession.value = missed
        }

        Result.Success(Unit)
    }

    override suspend fun endSession(): Result<JourneySession> = mutex.withLock {
        val current = _sessionStatus.value
        if (!current.canTransitionTo(SessionStatus.COMPLETING)) {
            return Result.Error(IllegalStateException("Cannot end session from status: $current"))
        }

        val session = _activeSession.value ?: return Result.Error(IllegalStateException("No active session found"))
        val now = clock.nowEpochMs()

        val completed = session.copy(
            status = SessionStatus.COMPLETED,
            endedAtEpochMs = now,
            checkpointState = CheckpointState.DISARMED
        )

        _activeSession.value = completed
        _sessionStatus.value = SessionStatus.COMPLETED
        Result.Success(completed)
    }

    override suspend fun cancelSession(): Result<JourneySession> = mutex.withLock {
        val current = _sessionStatus.value
        if (!current.canTransitionTo(SessionStatus.CANCELLED)) {
            return Result.Error(IllegalStateException("Cannot cancel session from status: $current"))
        }

        val session = _activeSession.value ?: return Result.Error(IllegalStateException("No active session found"))
        val now = clock.nowEpochMs()

        val cancelled = session.copy(
            status = SessionStatus.CANCELLED,
            endedAtEpochMs = now,
            checkpointState = CheckpointState.DISARMED
        )

        _activeSession.value = cancelled
        _sessionStatus.value = SessionStatus.CANCELLED
        Result.Success(cancelled)
    }

    override suspend fun resetToIdle(): Result<Unit> = mutex.withLock {
        val current = _sessionStatus.value
        if (!current.canTransitionTo(SessionStatus.IDLE)) {
            return Result.Error(IllegalStateException("Cannot reset to idle from status: $current"))
        }

        _activeSession.value = null
        _sessionStatus.value = SessionStatus.IDLE
        Result.Success(Unit)
    }

    override suspend fun restoreActiveSession(): Result<JourneySession?> = mutex.withLock {
        Result.Success(_activeSession.value)
    }

    /**
     * Updates the volatile, in-memory location of the active session without touching storage.
     */
    fun updateLocation(location: UserLocation) {
        val current = _activeSession.value ?: return
        if (current.status.isActiveSession) {
            _activeSession.value = current.copy(latestLocation = location)
        }
    }
}
