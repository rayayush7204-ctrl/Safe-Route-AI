package com.saferouteai.domain.model

/**
 * Domain-level journey state model.
 *
 * Designed to expand in future milestones to support full safety monitoring:
 * - POSSIBLE_JOURNEY
 * - AWAITING_CONFIRMATION
 * - WATCH
 * - HIGH_RISK
 * - INCIDENT
 * - RESOLVED
 *
 * In Milestone 1, only the foundational states (IDLE, ACTIVE, COMPLETED) are active.
 */
enum class JourneyState {
    /**
     * No active journey is taking place. System is idle.
     */
    IDLE,

    /**
     * Safe journey is currently in progress.
     */
    ACTIVE,

    /**
     * Safe journey has completed.
     */
    COMPLETED;

    /**
     * Verifies if a transition to [targetState] is valid from the current state.
     */
    fun canTransitionTo(targetState: JourneyState): Boolean = when (this) {
        IDLE -> targetState == ACTIVE
        ACTIVE -> targetState == COMPLETED
        COMPLETED -> targetState == IDLE || targetState == ACTIVE
    }
}
