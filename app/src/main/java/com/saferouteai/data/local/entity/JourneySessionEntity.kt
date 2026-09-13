package com.saferouteai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.saferouteai.domain.model.session.CheckpointState
import com.saferouteai.domain.model.session.JourneySession
import com.saferouteai.domain.model.session.SessionStatus

/**
 * Room database entity persisting Safe Journey session metadata.
 *
 * Notice: Transient/live location coordinates are explicitly omitted to prevent
 * storing historical location breadcrumbs, maintaining strict local privacy.
 */
@Entity(tableName = "journey_sessions")
data class JourneySessionEntity(
    @PrimaryKey
    val sessionId: String,
    val status: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long?,
    val origin: String?,
    val destination: String?,
    val lastCheckpointEpochMs: Long?,
    val nextCheckpointEpochMs: Long?,
    val checkpointState: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
) {
    /**
     * Converts this Room entity to a domain [JourneySession].
     * Note: [JourneySession.latestLocation] is transient and will be supplied by in-memory streams.
     */
    fun toDomainModel(): JourneySession {
        return JourneySession(
            sessionId = sessionId,
            status = runCatching { SessionStatus.valueOf(status) }.getOrDefault(SessionStatus.IDLE),
            startedAtEpochMs = startedAtEpochMs,
            endedAtEpochMs = endedAtEpochMs,
            origin = origin,
            destination = destination,
            latestLocation = null, // In-memory volatile
            lastCheckpointEpochMs = lastCheckpointEpochMs,
            nextCheckpointEpochMs = nextCheckpointEpochMs,
            checkpointState = runCatching { CheckpointState.valueOf(checkpointState) }
                .getOrDefault(CheckpointState.DISARMED)
        )
    }

    companion object {
        fun fromDomainModel(
            domain: JourneySession,
            createdAtEpochMs: Long,
            updatedAtEpochMs: Long
        ): JourneySessionEntity {
            return JourneySessionEntity(
                sessionId = domain.sessionId,
                status = domain.status.name,
                startedAtEpochMs = domain.startedAtEpochMs,
                endedAtEpochMs = domain.endedAtEpochMs,
                origin = domain.origin,
                destination = domain.destination,
                lastCheckpointEpochMs = domain.lastCheckpointEpochMs,
                nextCheckpointEpochMs = domain.nextCheckpointEpochMs,
                checkpointState = domain.checkpointState.name,
                createdAtEpochMs = createdAtEpochMs,
                updatedAtEpochMs = updatedAtEpochMs
            )
        }
    }
}
