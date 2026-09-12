package com.saferouteai.domain.model.location

/**
 * Immutable domain representation of geographic location coordinates.
 * Completely decoupled from android.location.Location.
 */
data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double? = null,
    val accuracyMeters: Float? = null,
    val speedMps: Float? = null,
    val bearingDegrees: Float? = null,
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val isApproximate: Boolean = false
)
