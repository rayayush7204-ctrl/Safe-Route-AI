package com.saferouteai.domain.model.session

/**
 * Typed domain errors occurring during Safe Journey session initialization.
 */
sealed class StartJourneyError {
    /**
     * User has not enabled explicit location sharing consent.
     */
    data object ConsentRequired : StartJourneyError()

    /**
     * Android runtime location permission is missing or denied.
     */
    data object PermissionRequired : StartJourneyError()

    /**
     * Session cannot be started from current status (e.g. already active).
     */
    data class InvalidState(val currentStatus: SessionStatus) : StartJourneyError()

    /**
     * Persistent storage or initialization error.
     */
    data class StorageError(val message: String) : StartJourneyError()
}
