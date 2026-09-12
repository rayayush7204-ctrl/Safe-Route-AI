package com.saferouteai.domain.model.location

/**
 * Domain-level error hierarchy for location tracking failures.
 */
sealed interface LocationError {
    val message: String

    data class ConsentNotGranted(
        override val message: String = "Location sharing consent has not been granted."
    ) : LocationError

    data class PermissionDenied(
        override val message: String = "Location permission is required."
    ) : LocationError

    data class ProviderDisabled(
        override val message: String = "Device location services are disabled."
    ) : LocationError

    data class TrackingFailed(
        override val message: String,
        val cause: Throwable? = null
    ) : LocationError
}
