package com.saferouteai.data.repository

import com.saferouteai.core.result.Result
import com.saferouteai.data.local.dao.JourneySessionDao
import com.saferouteai.data.local.entity.JourneySessionEntity
import com.saferouteai.domain.model.location.UserLocation
import com.saferouteai.domain.model.session.CheckpointPolicy
import com.saferouteai.domain.model.session.CheckpointState
import com.saferouteai.domain.model.session.JourneySession
import com.saferouteai.domain.model.session.SessionStatus
import com.saferouteai.domain.repository.JourneySessionRepository
import com.saferouteai.domain.repository.LocationRepository
import com.saferouteai.domain.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Production implementation of [JourneySessionRepository].
 *
 * Persists high-level session metadata and checkpoints to [JourneySessionDao] in Room,
 * while dynamically fusing live, volatile [UserLocation] updates from [LocationRepository]
 * into the in-memory [activeSession] flow without writing coordinates to disk.
 */
class RoomJourneySessionRepository(
    private val dao: JourneySessionDao,
    private val locationRepository: LocationRepository,
    private val clock: Clock,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : JourneySessionRepository {

    private val mutex = Mutex()

    private val _activeSession = MutableStateFlow<JourneySession?>(null)
    override val activeSession: StateFlow<JourneySession?> = _activeSession.asStateFlow()

    private val _sessionStatus = MutableStateFlow(SessionStatus.IDLE)
    override val sessionStatus: StateFlow<SessionStatus> = _sessionStatus.asStateFlow()

    private var activePolicy: CheckpointPolicy = CheckpointPolicy()

    init {
        // Collect live location updates and fuse into in-memory session without writing to Room
        externalScope.launch {
            locationRepository.currentLocation.collectLatest { location ->
                mutex.withLock {
                    val current = _activeSession.value
                    if (current != null && current.status.isActiveSession) {
                        _activeSession.value = current.copy(latestLocation = location)
                    }
                }
            }
        }
    }

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

        val domainSession = JourneySession(
            sessionId = UUID.randomUUID().toString(),
            status = SessionStatus.ACTIVE,
            startedAtEpochMs = now,
            endedAtEpochMs = null,
            origin = origin,
            destination = destination,
            latestLocation = locationRepository.currentLocation.value,
            lastCheckpointEpochMs = null,
            nextCheckpointEpochMs = nextCheckpoint,
            checkpointState = initialCheckpointState
        )

        val entity = JourneySessionEntity.fromDomainModel(
            domain = domainSession,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )

        try {
            dao.insertSession(entity)
            _activeSession.value = domainSession
            _sessionStatus.value = SessionStatus.ACTIVE
            Result.Success(domainSession)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun acknowledgeCheckpoint(): Result<JourneySession> = mutex.withLock {
        val current = _sessionStatus.value
        if (!current.canTransitionTo(SessionStatus.CHECKPOINT_ACKNOWLEDGED)) {
            return Result.Error(IllegalStateException("Cannot acknowledge checkpoint from status: $current"))
        }

        val session = _activeSession.value ?: return Result.Error(IllegalStateException("No active session found"))
        val now = clock.nowEpochMs()
        val nextCheckpoint = if (activePolicy.enabled) now + activePolicy.intervalMs else null

        val updatedDomain = session.copy(
            status = SessionStatus.ACTIVE,
            lastCheckpointEpochMs = now,
            nextCheckpointEpochMs = nextCheckpoint,
            checkpointState = if (activePolicy.enabled) CheckpointState.SCHEDULED else CheckpointState.DISARMED
        )

        val entity = JourneySessionEntity.fromDomainModel(
            domain = updatedDomain,
            createdAtEpochMs = updatedDomain.startedAtEpochMs,
            updatedAtEpochMs = now
        )

        try {
            dao.updateSession(entity)
            _activeSession.value = updatedDomain
            _sessionStatus.value = SessionStatus.ACTIVE
            Result.Success(updatedDomain)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun triggerCheckpointDue(): Result<JourneySession> = mutex.withLock {
        val current = _sessionStatus.value
        if (!current.canTransitionTo(SessionStatus.CHECKPOINT_DUE)) {
            return Result.Error(IllegalStateException("Cannot trigger checkpoint due from status: $current"))
        }

        val session = _activeSession.value ?: return Result.Error(IllegalStateException("No active session found"))
        val now = clock.nowEpochMs()

        val updatedDomain = session.copy(
            status = SessionStatus.CHECKPOINT_DUE,
            checkpointState = CheckpointState.DUE
        )

        val entity = JourneySessionEntity.fromDomainModel(
            domain = updatedDomain,
            createdAtEpochMs = updatedDomain.startedAtEpochMs,
            updatedAtEpochMs = now
        )

        try {
            dao.updateSession(entity)
            _activeSession.value = updatedDomain
            _sessionStatus.value = SessionStatus.CHECKPOINT_DUE
            Result.Success(updatedDomain)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun checkCheckpointExpiry(): Result<Unit> = mutex.withLock {
        val session = _activeSession.value ?: return Result.Success(Unit)
        if (session.status != SessionStatus.CHECKPOINT_DUE) return Result.Success(Unit)

        val nextCheckpoint = session.nextCheckpointEpochMs ?: return Result.Success(Unit)
        val expiryTime = nextCheckpoint + activePolicy.acknowledgementWindowMs
        val now = clock.nowEpochMs()

        if (now > expiryTime) {
            val missedDomain = session.copy(
                checkpointState = CheckpointState.MISSED
            )
            val entity = JourneySessionEntity.fromDomainModel(
                domain = missedDomain,
                createdAtEpochMs = missedDomain.startedAtEpochMs,
                updatedAtEpochMs = now
            )
            try {
                dao.updateSession(entity)
                _activeSession.value = missedDomain
            } catch (e: Exception) {
                return Result.Error(e)
            }
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

        val completedDomain = session.copy(
            status = SessionStatus.COMPLETED,
            endedAtEpochMs = now,
            checkpointState = CheckpointState.DISARMED
        )

        val entity = JourneySessionEntity.fromDomainModel(
            domain = completedDomain,
            createdAtEpochMs = completedDomain.startedAtEpochMs,
            updatedAtEpochMs = now
        )

        try {
            dao.updateSession(entity)
            _activeSession.value = completedDomain
            _sessionStatus.value = SessionStatus.COMPLETED
            Result.Success(completedDomain)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun cancelSession(): Result<JourneySession> = mutex.withLock {
        val current = _sessionStatus.value
        if (!current.canTransitionTo(SessionStatus.CANCELLED)) {
            return Result.Error(IllegalStateException("Cannot cancel session from status: $current"))
        }

        val session = _activeSession.value ?: return Result.Error(IllegalStateException("No active session found"))
        val now = clock.nowEpochMs()

        val cancelledDomain = session.copy(
            status = SessionStatus.CANCELLED,
            endedAtEpochMs = now,
            checkpointState = CheckpointState.DISARMED
        )

        val entity = JourneySessionEntity.fromDomainModel(
            domain = cancelledDomain,
            createdAtEpochMs = cancelledDomain.startedAtEpochMs,
            updatedAtEpochMs = now
        )

        try {
            dao.updateSession(entity)
            _activeSession.value = cancelledDomain
            _sessionStatus.value = SessionStatus.CANCELLED
            Result.Success(cancelledDomain)
        } catch (e: Exception) {
            Result.Error(e)
        }
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
        try {
            val activeEntity = dao.getActiveSession()
            if (activeEntity != null) {
                val domain = activeEntity.toDomainModel().copy(
                    latestLocation = locationRepository.currentLocation.value
                )
                _activeSession.value = domain
                _sessionStatus.value = domain.status
                Result.Success(domain)
            } else {
                Result.Success(null)
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
