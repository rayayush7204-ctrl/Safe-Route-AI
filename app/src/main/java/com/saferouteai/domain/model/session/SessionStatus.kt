package com.saferouteai.domain.model.session

/**
 * Strongly typed 8-state machine for Safe Journey Sessions.
 *
 * State flow:
 * IDLE -> STARTING -> ACTIVE -> CHECKPOINT_DUE -> CHECKPOINT_ACKNOWLEDGED -> ACTIVE -> COMPLETING -> COMPLETED -> IDLE
 * Any active/starting state can also transition to CANCELLED.
 */
enum class SessionStatus {
    /**
     * No active session. System is at rest.
     */
    IDLE,

    /**
     * Session initialization in progress (preflight validation, resource allocation).
     */
    STARTING,

    /**
     * Safe Journey is active and monitored.
     */
    ACTIVE,

    /**
     * A periodic safety checkpoint is due for user confirmation.
     */
    CHECKPOINT_DUE,

    /**
     * The user has confirmed safety ("I'm Okay"); preparing next interval.
     */
    CHECKPOINT_ACKNOWLEDGED,

    /**
     * Session conclusion initiated; tearing down listeners.
     */
    COMPLETING,

    /**
     * Session successfully concluded.
     */
    COMPLETED,

    /**
     * Session cancelled by user before normal completion.
     */
    CANCELLED;

    /**
     * Validates if a transition from this state to [target] is permitted.
     */
    fun canTransitionTo(target: SessionStatus): Boolean = when (this) {
        IDLE -> target == STARTING
        STARTING -> target == ACTIVE || target == CANCELLED
        ACTIVE -> target == CHECKPOINT_DUE || target == COMPLETING || target == CANCELLED
        CHECKPOINT_DUE -> target == CHECKPOINT_ACKNOWLEDGED || target == COMPLETING || target == CANCELLED
        CHECKPOINT_ACKNOWLEDGED -> target == ACTIVE || target == COMPLETING || target == CANCELLED
        COMPLETING -> target == COMPLETED
        COMPLETED -> target == IDLE
        CANCELLED -> target == IDLE
    }

    /**
     * True if this state represents an ongoing, un-terminated session.
     */
    val isActiveSession: Boolean
        get() = this in setOf(STARTING, ACTIVE, CHECKPOINT_DUE, CHECKPOINT_ACKNOWLEDGED, COMPLETING)
}
