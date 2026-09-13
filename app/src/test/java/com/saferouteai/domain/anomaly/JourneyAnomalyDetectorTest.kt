package com.saferouteai.domain.anomaly

import com.saferouteai.domain.model.anomaly.AnomalyDetectionPolicy
import com.saferouteai.domain.model.anomaly.AnomalyType
import com.saferouteai.domain.model.anomaly.ExpectedRoute
import com.saferouteai.domain.model.anomaly.RoutePoint
import com.saferouteai.domain.model.location.UserLocation
import com.saferouteai.domain.model.session.JourneySession
import com.saferouteai.domain.model.session.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JourneyAnomalyDetectorTest {

    private lateinit var detector: JourneyAnomalyDetector
    private lateinit var activeSession: JourneySession
    private val policy = AnomalyDetectionPolicy(
        prolongedStopThresholdMs = 600_000L, // 10 mins
        stationaryRadiusMeters = 30.0,
        routeDeviationThresholdMeters = 100.0,
        expectedDurationMultiplier = 1.5,
        maxAcceptableAccuracyMeters = 40.0f,
        minObservationsForStop = 3,
        unusualMovementDirectionReversalsThreshold = 3
    )

    @Before
    fun setUp() {
        detector = JourneyAnomalyDetector(policy)
        activeSession = JourneySession(
            sessionId = "session-123",
            status = SessionStatus.ACTIVE,
            startedAtEpochMs = 1_000_000L
        )
    }

    // ==========================================
    // Signal 1: PROLONGED_STOP
    // ==========================================

    @Test
    fun `stationary duration below threshold emits no prolonged stop anomaly`() {
        val locations = listOf(
            UserLocation(latitude = 37.7749, longitude = -122.4194, timestampEpochMs = 1_000_000L, accuracyMeters = 5f),
            UserLocation(latitude = 37.77491, longitude = -122.41941, timestampEpochMs = 1_200_000L, accuracyMeters = 5f),
            UserLocation(latitude = 37.77492, longitude = -122.41942, timestampEpochMs = 1_400_000L, accuracyMeters = 5f)
        )

        // Only 400s (6.6 mins) elapsed; threshold is 600s (10 mins)
        val signals = detector.detectAnomalies(
            session = activeSession,
            recentLocations = locations,
            currentTimeEpochMs = 1_400_000L
        )

        assertTrue(signals.none { it.type == AnomalyType.PROLONGED_STOP })
    }

    @Test
    fun `stationary duration exceeding threshold emits PROLONGED_STOP`() {
        val locations = listOf(
            UserLocation(latitude = 37.7749, longitude = -122.4194, timestampEpochMs = 1_000_000L, accuracyMeters = 5f),
            UserLocation(latitude = 37.77491, longitude = -122.41941, timestampEpochMs = 1_300_000L, accuracyMeters = 5f),
            UserLocation(latitude = 37.77492, longitude = -122.41942, timestampEpochMs = 1_650_000L, accuracyMeters = 5f)
        )

        // 650s elapsed within 5m displacement; threshold is 600s
        val signals = detector.detectAnomalies(
            session = activeSession,
            recentLocations = locations,
            currentTimeEpochMs = 1_650_000L
        )

        val stopSignal = signals.find { it.type == AnomalyType.PROLONGED_STOP }
        assertNotNull(stopSignal)
        assertEquals("session-123", stopSignal?.sessionId)
        assertTrue(stopSignal?.explanation?.contains("stationary") == true)
    }

    @Test
    fun `moving beyond stationary radius prevents PROLONGED_STOP`() {
        val locations = listOf(
            UserLocation(latitude = 37.7749, longitude = -122.4194, timestampEpochMs = 1_000_000L, accuracyMeters = 5f),
            UserLocation(latitude = 37.7749, longitude = -122.4194, timestampEpochMs = 1_300_000L, accuracyMeters = 5f),
            // Moved ~500 meters away at 1_650_000L
            UserLocation(latitude = 37.7794, longitude = -122.4194, timestampEpochMs = 1_650_000L, accuracyMeters = 5f)
        )

        val signals = detector.detectAnomalies(
            session = activeSession,
            recentLocations = locations,
            currentTimeEpochMs = 1_650_000L
        )

        assertTrue(signals.none { it.type == AnomalyType.PROLONGED_STOP })
    }

    // ==========================================
    // Signal 2: ROUTE_DEVIATION
    // ==========================================

    @Test
    fun `location within route corridor emits no route deviation`() {
        val route = ExpectedRoute(
            routeId = "route-1",
            waypoints = listOf(
                RoutePoint(37.7749, -122.4194),
                RoutePoint(37.7849, -122.4194)
            ),
            corridorRadiusMeters = 100.0
        )

        // Point is ~30m off corridor
        val locations = listOf(
            UserLocation(latitude = 37.7799, longitude = -122.41905, timestampEpochMs = 1_100_000L, accuracyMeters = 5f)
        )

        val signals = detector.detectAnomalies(
            session = activeSession,
            recentLocations = locations,
            expectedRoute = route,
            currentTimeEpochMs = 1_100_000L
        )

        assertTrue(signals.none { it.type == AnomalyType.ROUTE_DEVIATION })
    }

    @Test
    fun `location beyond route corridor emits ROUTE_DEVIATION`() {
        val route = ExpectedRoute(
            routeId = "route-1",
            waypoints = listOf(
                RoutePoint(37.7749, -122.4194),
                RoutePoint(37.7849, -122.4194)
            ),
            corridorRadiusMeters = 100.0
        )

        // Point is ~260m East of route (approx 0.003 deg longitude)
        val locations = listOf(
            UserLocation(latitude = 37.7799, longitude = -122.4164, timestampEpochMs = 1_100_000L, accuracyMeters = 5f)
        )

        val signals = detector.detectAnomalies(
            session = activeSession,
            recentLocations = locations,
            expectedRoute = route,
            currentTimeEpochMs = 1_100_000L
        )

        val deviation = signals.find { it.type == AnomalyType.ROUTE_DEVIATION }
        assertNotNull(deviation)
        assertTrue(deviation?.explanation?.contains("outside expected route corridor") == true)
    }

    @Test
    fun `route deviation is not emitted when GPS accuracy is too poor`() {
        val route = ExpectedRoute(
            routeId = "route-1",
            waypoints = listOf(RoutePoint(37.7749, -122.4194), RoutePoint(37.7849, -122.4194)),
            corridorRadiusMeters = 100.0
        )

        // Location is off corridor, but accuracy is 80m (exceeds 40m ceiling)
        val locations = listOf(
            UserLocation(latitude = 37.7799, longitude = -122.4164, timestampEpochMs = 1_100_000L, accuracyMeters = 80f)
        )

        val signals = detector.detectAnomalies(
            session = activeSession,
            recentLocations = locations,
            expectedRoute = route,
            currentTimeEpochMs = 1_100_000L
        )

        assertTrue(signals.none { it.type == AnomalyType.ROUTE_DEVIATION })
    }

    // ==========================================
    // Signal 3: DURATION_ANOMALY
    // ==========================================

    @Test
    fun `journey within expected duration multiplier emits no duration anomaly`() {
        // Expected: 20 mins (1_200_000ms), 1.5x threshold is 30 mins
        // Actual elapsed: 25 mins (1_500_000ms)
        val signals = detector.detectAnomalies(
            session = activeSession.copy(startedAtEpochMs = 1_000_000L),
            recentLocations = emptyList(),
            expectedDurationMs = 1_200_000L,
            currentTimeEpochMs = 2_500_000L // 25 min elapsed
        )

        assertTrue(signals.none { it.type == AnomalyType.DURATION_ANOMALY })
    }

    @Test
    fun `journey exceeding expected duration multiplier emits DURATION_ANOMALY`() {
        // Expected: 20 mins (1_200_000ms), 1.5x threshold is 30 mins (1_800_000ms)
        // Actual elapsed: 35 mins (2_100_000ms)
        val signals = detector.detectAnomalies(
            session = activeSession.copy(startedAtEpochMs = 1_000_000L),
            recentLocations = emptyList(),
            expectedDurationMs = 1_200_000L,
            currentTimeEpochMs = 3_100_000L // 35 min elapsed
        )

        val durationSignal = signals.find { it.type == AnomalyType.DURATION_ANOMALY }
        assertNotNull(durationSignal)
        assertTrue(durationSignal?.explanation?.contains("exceeds planned time") == true)
    }

    // ==========================================
    // Signal 4: UNUSUAL_MOVEMENT
    // ==========================================

    @Test
    fun `linear progression emits no unusual movement anomaly`() {
        val locations = listOf(
            UserLocation(37.7700, -122.4194, timestampEpochMs = 100_000L, accuracyMeters = 5f),
            UserLocation(37.7710, -122.4194, timestampEpochMs = 110_000L, accuracyMeters = 5f),
            UserLocation(37.7720, -122.4194, timestampEpochMs = 120_000L, accuracyMeters = 5f),
            UserLocation(37.7730, -122.4194, timestampEpochMs = 130_000L, accuracyMeters = 5f),
            UserLocation(37.7740, -122.4194, timestampEpochMs = 140_000L, accuracyMeters = 5f)
        )

        val signals = detector.detectAnomalies(
            session = activeSession,
            recentLocations = locations,
            currentTimeEpochMs = 150_000L
        )

        assertTrue(signals.none { it.type == AnomalyType.UNUSUAL_MOVEMENT })
    }

    @Test
    fun `repeated sharp heading reversals emit UNUSUAL_MOVEMENT`() {
        // Alternating back and forth between point A and B (~40m apart)
        val locations = listOf(
            UserLocation(37.7700, -122.4194, timestampEpochMs = 100_000L, accuracyMeters = 5f),
            UserLocation(37.7704, -122.4194, timestampEpochMs = 110_000L, accuracyMeters = 5f), // North (0 deg)
            UserLocation(37.7700, -122.4194, timestampEpochMs = 120_000L, accuracyMeters = 5f), // South (180 deg) -> reversal 1
            UserLocation(37.7704, -122.4194, timestampEpochMs = 130_000L, accuracyMeters = 5f), // North (0 deg) -> reversal 2
            UserLocation(37.7700, -122.4194, timestampEpochMs = 140_000L, accuracyMeters = 5f), // South (180 deg) -> reversal 3
            UserLocation(37.7704, -122.4194, timestampEpochMs = 150_000L, accuracyMeters = 5f)  // North (0 deg) -> reversal 4
        )

        val signals = detector.detectAnomalies(
            session = activeSession,
            recentLocations = locations,
            currentTimeEpochMs = 160_000L
        )

        val movementSignal = signals.find { it.type == AnomalyType.UNUSUAL_MOVEMENT }
        assertNotNull(movementSignal)
        assertTrue(movementSignal?.explanation?.contains("direction reversals") == true)
    }

    // ==========================================
    // Data Quality & Robustness
    // ==========================================

    @Test
    fun `handles empty or single-point location history gracefully`() {
        val emptySignals = detector.detectAnomalies(activeSession, emptyList(), currentTimeEpochMs = 1_000_000L)
        assertEquals(0, emptySignals.size)

        val singleSignals = detector.detectAnomalies(
            activeSession,
            listOf(UserLocation(37.7749, -122.4194, timestampEpochMs = 1_000_000L)),
            currentTimeEpochMs = 1_000_000L
        )
        assertEquals(0, singleSignals.size)
    }

    @Test
    fun `handles invalid coordinates and out of order timestamps without crashing`() {
        val corrupted = listOf(
            UserLocation(37.7749, -122.4194, timestampEpochMs = 1_050_000L),
            UserLocation(120.0, -122.4194, timestampEpochMs = 1_060_000L), // invalid lat > 90
            UserLocation(37.7749, -200.0, timestampEpochMs = 1_070_000L),  // invalid lon < -180
            UserLocation(37.7750, -122.4195, timestampEpochMs = 1_010_000L), // out of order
            UserLocation(37.7751, -122.4196, timestampEpochMs = -500L)      // negative timestamp
        )

        val signals = detector.detectAnomalies(
            activeSession,
            corrupted,
            currentTimeEpochMs = 1_080_000L
        )

        // Must not crash and should filter bad data safely
        assertTrue(signals.isEmpty())
    }

    @Test
    fun `no anomalies emitted when session is idle or completed`() {
        val idleSession = activeSession.copy(status = SessionStatus.IDLE)
        val locations = listOf(
            UserLocation(37.7749, -122.4194, timestampEpochMs = 1_000_000L, accuracyMeters = 5f),
            UserLocation(37.7749, -122.4194, timestampEpochMs = 2_000_000L, accuracyMeters = 5f)
        )

        val signals = detector.detectAnomalies(idleSession, locations, currentTimeEpochMs = 2_000_000L)
        assertTrue(signals.isEmpty())
    }
}
