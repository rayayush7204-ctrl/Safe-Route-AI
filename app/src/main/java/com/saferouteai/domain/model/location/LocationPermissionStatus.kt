package com.saferouteai.domain.model.location

/**
 * Domain-level representation of runtime location permission status.
 */
enum class LocationPermissionStatus {
    NOT_REQUESTED,
    DENIED,
    PERMANENTLY_DENIED,
    COARSE_GRANTED,
    FINE_GRANTED;

    val isGranted: Boolean
        get() = this == COARSE_GRANTED || this == FINE_GRANTED

    val isPrecise: Boolean
        get() = this == FINE_GRANTED
}
