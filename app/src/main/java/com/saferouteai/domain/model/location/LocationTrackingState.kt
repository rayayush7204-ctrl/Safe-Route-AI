package com.saferouteai.domain.model.location

/**
 * State machine managing foreground location tracking lifecycle.
 */
enum class LocationTrackingState {
    /**
     * Resting state. Zero GPS/sensor polling.
     */
    IDLE,

    /**
     * User has not enabled locationSharingConsent. Consent must be granted.
     */
    CONSENT_REQUIRED,

    /**
     * Consent is granted, but Android runtime permission has not been granted.
     */
    PERMISSION_REQUIRED,

    /**
     * Both gates (consent + runtime permission) are satisfied. Ready to start.
     */
    READY,

    /**
     * Active foreground location streaming.
     */
    TRACKING,

    /**
     * Temporary interruption (e.g. Activity left the foreground).
     * No location updates are emitted while PAUSED.
     */
    PAUSED,

    /**
     * Provider disabled (GPS off) or hardware error.
     */
    ERROR,

    /**
     * Tracking explicitly stopped. Hardware listeners detached.
     */
    STOPPED;

    fun canTransitionTo(target: LocationTrackingState): Boolean = when (this) {
        IDLE -> target in setOf(CONSENT_REQUIRED, PERMISSION_REQUIRED, READY, TRACKING)
        CONSENT_REQUIRED -> target in setOf(PERMISSION_REQUIRED, READY, IDLE)
        PERMISSION_REQUIRED -> target in setOf(READY, IDLE, ERROR)
        READY -> target in setOf(TRACKING, IDLE, STOPPED)
        TRACKING -> target in setOf(PAUSED, STOPPED, ERROR)
        PAUSED -> target in setOf(TRACKING, STOPPED, ERROR)
        ERROR -> target in setOf(READY, PERMISSION_REQUIRED, IDLE, STOPPED)
        STOPPED -> target in setOf(IDLE, READY)
    }
}
