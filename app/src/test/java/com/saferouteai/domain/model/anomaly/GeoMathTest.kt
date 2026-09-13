package com.saferouteai.domain.model.anomaly

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoMathTest {

    @Test
    fun `distance between identical coordinates is zero`() {
        val dist = GeoMath.distanceBetweenMeters(37.7749, -122.4194, 37.7749, -122.4194)
        assertEquals(0.0, dist, 0.001)
    }

    @Test
    fun `distance between San Francisco and Los Angeles is approx 559km`() {
        // SF: 37.7749, -122.4194; LA: 34.0522, -118.2437
        val dist = GeoMath.distanceBetweenMeters(37.7749, -122.4194, 34.0522, -118.2437)
        // Standard expected great-circle distance is ~559 km (+/- 3km)
        assertEquals(559_000.0, dist, 3_000.0)
    }

    @Test
    fun `bearing due North is 0 degrees and due East is 90 degrees`() {
        val north = GeoMath.bearingBetweenDegrees(37.0, -122.0, 38.0, -122.0)
        assertEquals(0.0f, north, 1.0f)

        val east = GeoMath.bearingBetweenDegrees(37.0, -122.0, 37.0, -121.0)
        assertEquals(90.0f, east, 1.0f)
    }

    @Test
    fun `angle difference calculates acute difference`() {
        assertEquals(0.0f, GeoMath.angleDifferenceDegrees(10f, 10f), 0.01f)
        assertEquals(90.0f, GeoMath.angleDifferenceDegrees(0f, 90f), 0.01f)
        assertEquals(180.0f, GeoMath.angleDifferenceDegrees(0f, 180f), 0.01f)
        assertEquals(10.0f, GeoMath.angleDifferenceDegrees(5f, 355f), 0.01f)
    }

    @Test
    fun `distance to segment when point is directly on segment is zero`() {
        // Segment from (0, 0) to (0, 1)
        val dist = GeoMath.distanceToSegmentMeters(
            pLat = 0.0, pLon = 0.5,
            aLat = 0.0, aLon = 0.0,
            bLat = 0.0, bLon = 1.0
        )
        assertEquals(0.0, dist, 1.0)
    }

    @Test
    fun `distance to polyline finds closest distance along corridor`() {
        val waypoints = listOf(
            RoutePoint(latitude = 37.7749, longitude = -122.4194, order = 0),
            RoutePoint(latitude = 37.7849, longitude = -122.4194, order = 1) // ~1.11 km North
        )

        // Point is ~111m East of the midpoint
        val pointLat = 37.7799
        val pointLon = -122.41813 // ~111m East
        val dist = GeoMath.distanceToPolylineMeters(pointLat, pointLon, waypoints)
        assertTrue(dist > 90.0 && dist < 130.0)
    }
}
