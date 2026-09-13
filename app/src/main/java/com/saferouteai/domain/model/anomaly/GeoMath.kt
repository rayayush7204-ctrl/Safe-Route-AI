package com.saferouteai.domain.model.anomaly

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure Kotlin geodesic and spherical geometry calculations.
 *
 * Fully decoupled from android.location.Location or Google Maps APIs, ensuring
 * 100% deterministic pure-JVM testability.
 */
object GeoMath {

    const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates the great-circle geodesic distance between two points on Earth using the Haversine formula.
     */
    fun distanceBetweenMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Computes the initial forward compass bearing from point (lat1, lon1) to (lat2, lon2) in degrees [0, 360).
     */
    fun bearingBetweenDegrees(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = atan2(y, x)
        val bearing = (Math.toDegrees(theta) + 360.0) % 360.0
        return bearing.toFloat()
    }

    /**
     * Calculates the angular difference between two bearings in degrees [0, 180].
     */
    fun angleDifferenceDegrees(bearing1: Float, bearing2: Float): Float {
        val diff = abs(bearing1 - bearing2) % 360f
        return if (diff > 180f) 360f - diff else diff
    }

    /**
     * Calculates the minimum perpendicular/shortest distance from a point to a polyline formed by [waypoints].
     *
     * @return Minimum distance in meters, or [Double.MAX_VALUE] if waypoints is empty.
     */
    fun distanceToPolylineMeters(
        pointLat: Double,
        pointLon: Double,
        waypoints: List<RoutePoint>
    ): Double {
        if (waypoints.isEmpty()) return Double.MAX_VALUE
        if (waypoints.size == 1) {
            return distanceBetweenMeters(pointLat, pointLon, waypoints[0].latitude, waypoints[0].longitude)
        }

        var minDistance = Double.MAX_VALUE

        for (i in 0 until waypoints.size - 1) {
            val a = waypoints[i]
            val b = waypoints[i + 1]
            val segDist = distanceToSegmentMeters(pointLat, pointLon, a.latitude, a.longitude, b.latitude, b.longitude)
            if (segDist < minDistance) {
                minDistance = segDist
            }
        }

        return minDistance
    }

    /**
     * Calculates the shortest distance in meters from point P to line segment AB using local planar projection.
     */
    fun distanceToSegmentMeters(
        pLat: Double,
        pLon: Double,
        aLat: Double,
        aLon: Double,
        bLat: Double,
        bLon: Double
    ): Double {
        val midLatRad = Math.toRadians((aLat + bLat) / 2.0)
        val metersPerDegLat = 111132.954
        val metersPerDegLon = 111132.954 * cos(midLatRad)

        // Convert coordinates to local Cartesian displacement in meters relative to A
        val bx = (bLon - aLon) * metersPerDegLon
        val by = (bLat - aLat) * metersPerDegLat

        val px = (pLon - aLon) * metersPerDegLon
        val py = (pLat - aLat) * metersPerDegLat

        val segLengthSq = bx * bx + by * by

        if (segLengthSq < 1e-6) {
            // A and B are effectively the same point
            return sqrt(px * px + py * py)
        }

        // Projection scalar t of P onto segment AB
        val t = ((px * bx + py * by) / segLengthSq).coerceIn(0.0, 1.0)

        val closestX = t * bx
        val closestY = t * by

        val dx = px - closestX
        val dy = py - closestY

        return sqrt(dx * dx + dy * dy)
    }
}
