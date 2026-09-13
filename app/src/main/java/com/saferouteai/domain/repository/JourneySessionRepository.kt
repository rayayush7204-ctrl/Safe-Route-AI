package com.saferouteai.domain.repository

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.session.CheckpointPolicy
import com.saferouteai.domain.model.session.JourneySession
import com.saferouteai.domain.model.session.SessionStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain repository contract for managing Safe Journey sessions, their lifecycle,
 * and checkpoint state.
 */
interface JourneySessionRepository {

    /**
     * Observable stream of the currently active or latest session, or null if idle.
     */
    val activeSession: StateFlow<JourneySession?>

    /**
     * Observable stream of the current [SessionStatus].
     */
    val sessionStatus: StateFlow<SessionStatus>

    /**
     * Initiates a new Safe Journey session.
     * Transitions IDLE -> STARTING -> ACTIVE (if gates allow) with initial checkpoint scheduled.
     *
     * @param origin Optional start location label.
     * @param destination Optional destination label.
     * @param policy Checkpoint configuration policy.
     * @return Result containing the initialized [JourneySession], or error if transition disallowed.
     */
    suspend fun startSession(
        origin: String? = null,
        destination: String? = null,
        policy: CheckpointPolicy = CheckpointPolicy()
    ): Result<JourneySession>

    /**
     * Acknowledges a due checkpoint ("I'm Okay").
     * Transitions CHECKPOINT_DUE -> CHECKPOINT_ACKNOWLEDGED -> ACTIVE and schedules the next checkpoint.
     */
    suspend fun acknowledgeCheckpoint(): Result<JourneySession>

    /**
     * Flags the current active session's checkpoint as due.
     * Transitions ACTIVE -> CHECKPOINT_DUE.
     */
    suspend fun triggerCheckpointDue(): Result<JourneySession>

    /**
     * Evaluates whether the acknowledgement window has elapsed for a due checkpoint.
     * If expired, marks the checkpointState as MISSED (without emergency escalation).
     */
    suspend fun checkCheckpointExpiry(): Result<Unit>

    /**
     * Concludes the active safe journey normally.
     * Transitions ACTIVE/CHECKPOINT_* -> COMPLETING -> COMPLETED.
     */
    suspend fun endSession(): Result<JourneySession>

    /**
     * Cancels the active or starting safe journey.
     * Transitions to CANCELLED.
     */
    suspend fun cancelSession(): Result<JourneySession>

    /**
     * Resets terminal session state back to [SessionStatus.IDLE].
     */
    suspend fun resetToIdle(): Result<Unit>

    /**
     * Inspects persistent storage on startup to restore an uncompleted active session if present.
     */
    suspend fun restoreActiveSession(): Result<JourneySession?>
}
