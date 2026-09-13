package com.saferouteai.domain.model.session

import com.saferouteai.domain.model.location.UserLocation
import com.saferouteai.domain.time.Clock

/**
 * Domain model representing a Safe Journey Session.
 *
 * @property sessionId Unique identifier for this session.
 * @property status Current state in the session state machine.
 * @property startedAtEpochMs Epoch timestamp when the session was started.
 * @property endedAtEpochMs Epoch timestamp when the session ended or was cancelled, or null if active.
 * @property origin Optional starting address/label.
 * @property destination Optional target destination.
 * @property latestLocation Volatile in-memory location update (never persisted to Room).
 * @property lastCheckpointEpochMs Timestamp of last acknowledged/triggered checkpoint.
 * @property nextCheckpointEpochMs Timestamp of upcoming checkpoint.
 * @property checkpointState Current status of local checkpoint engine.
 */
data class JourneySession(
    val sessionId: String,
    val status: SessionStatus,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long? = null,
    val origin: String? = null,
    val destination: String? = null,
    val latestLocation: UserLocation? = null,
    val lastCheckpointEpochMs: Long? = null,
    val nextCheckpointEpochMs: Long? = null,
    val checkpointState: CheckpointState = CheckpointState.DISARMED
) {
    /**
     * Computes the elapsed duration of the journey using the provided [clock].
     */
    fun calculateDurationMs(clock: Clock): Long {
        val referenceEnd = endedAtEpochMs ?: clock.nowEpochMs()
        return (referenceEnd - startedAtEpochMs).coerceAtLeast(0L)
    }
}
