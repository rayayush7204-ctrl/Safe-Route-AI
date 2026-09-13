package com.saferouteai.domain.model.anomaly

/**
 * An ordered geographic coordinate forming a segment of a planned route.
 */
data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val order: Int = 0
)

/**
 * Pure Kotlin abstraction representing a planned route corridor.
 *
 * Decoupled from Google Maps SDK and Google Directions/Routes APIs.
 *
 * @property routeId Identifier for the expected path.
 * @property waypoints Ordered sequence of points defining the route path.
 * @property corridorRadiusMeters Allowed lateral deviation buffer in meters.
 */
data class ExpectedRoute(
    val routeId: String,
    val waypoints: List<RoutePoint>,
    val corridorRadiusMeters: Double = DEFAULT_CORRIDOR_METERS
) {
    companion object {
        const val DEFAULT_CORRIDOR_METERS = 100.0
    }
}
