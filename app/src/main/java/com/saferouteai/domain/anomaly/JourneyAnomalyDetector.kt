package com.saferouteai.domain.anomaly

import com.saferouteai.domain.model.anomaly.AnomalyDetectionPolicy
import com.saferouteai.domain.model.anomaly.AnomalySeverity
import com.saferouteai.domain.model.anomaly.AnomalySignal
import com.saferouteai.domain.model.anomaly.AnomalyType
import com.saferouteai.domain.model.anomaly.ExpectedRoute
import com.saferouteai.domain.model.anomaly.GeoMath
import com.saferouteai.domain.model.location.UserLocation
import com.saferouteai.domain.model.session.JourneySession
import java.util.UUID

/**
 * Deterministic, side-effect-free, explainable local journey anomaly detection engine.
 *
 * Evaluates active safe journey telemetry against configured policy rules to produce
 * typed [AnomalySignal] observations without machine learning or network dependencies.
 */
class JourneyAnomalyDetector(
    private val policy: AnomalyDetectionPolicy = AnomalyDetectionPolicy()
) {

    /**
     * Analyzes current session and recent locations to identify any active anomaly signals.
     *
     * @param session Active safe journey session.
     * @param recentLocations Recent volatile location observations (most recent last).
     * @param expectedRoute Optional planned route corridor.
     * @param expectedDurationMs Optional target duration for the session in milliseconds.
     * @param currentTimeEpochMs Current timestamp supplied by injectable Clock.
     * @return List of detected [AnomalySignal] instances.
     */
    fun detectAnomalies(
        session: JourneySession,
        recentLocations: List<UserLocation>,
        expectedRoute: ExpectedRoute? = null,
        expectedDurationMs: Long? = null,
        currentTimeEpochMs: Long = System.currentTimeMillis()
    ): List<AnomalySignal> {
        // Invariant: Anomalies are only evaluated during active sessions
        if (!session.status.isActiveSession) {
            return emptyList()
        }

        val signals = mutableListOf<AnomalySignal>()

        // Sanitize and filter location observations
        val validLocations = sanitizeLocations(recentLocations, currentTimeEpochMs)

        // Rule 1: Prolonged Stop
        detectProlongedStop(session.sessionId, validLocations, currentTimeEpochMs)?.let { signals.add(it) }

        // Rule 2: Route Deviation
        detectRouteDeviation(session.sessionId, validLocations, expectedRoute, currentTimeEpochMs)?.let { signals.add(it) }

        // Rule 3: Journey Duration Anomaly
        detectDurationAnomaly(session, expectedDurationMs, currentTimeEpochMs)?.let { signals.add(it) }

        // Rule 4: Unusual Movement Pattern
        detectUnusualMovement(session.sessionId, validLocations, currentTimeEpochMs)?.let { signals.add(it) }

        return signals
    }

    /**
     * Filters out impossible coordinates, future-skewed timestamps, and sorts ascending by time.
     */
    private fun sanitizeLocations(
        locations: List<UserLocation>,
        currentTimeEpochMs: Long
    ): List<UserLocation> {
        return locations
            .filter { loc ->
                loc.latitude in -90.0..90.0 &&
                        loc.longitude in -180.0..180.0 &&
                        loc.timestampEpochMs > 0 &&
                        loc.timestampEpochMs <= currentTimeEpochMs + 30_000L // Max 30s clock skew tolerance
            }
            .sortedBy { it.timestampEpochMs }
    }

    /**
     * Signal 1: PROLONGED_STOP
     * Triggers when user has remained stationary within [policy.stationaryRadiusMeters]
     * for at least [policy.prolongedStopThresholdMs].
     */
    private fun detectProlongedStop(
        sessionId: String,
        locations: List<UserLocation>,
        currentTimeEpochMs: Long
    ): AnomalySignal? {
        val accurateLocations = locations.filter {
            it.accuracyMeters == null || it.accuracyMeters <= policy.maxAcceptableAccuracyMeters
        }

        if (accurateLocations.size < policy.minObservationsForStop) return null

        val latest = accurateLocations.last()

        // Scan backwards to find the earliest point from which all subsequent points remain within stationary radius
        var earliestStationary = latest
        var allWithinRadius = true

        for (i in accurateLocations.indices.reversed()) {
            val candidate = accurateLocations[i]
            val dist = GeoMath.distanceBetweenMeters(
                candidate.latitude, candidate.longitude,
                latest.latitude, latest.longitude
            )
            if (dist <= policy.stationaryRadiusMeters) {
                earliestStationary = candidate
            } else {
                allWithinRadius = false
                break
            }
        }

        val stationaryDurationMs = currentTimeEpochMs - earliestStationary.timestampEpochMs
        val pointsInWindow = accurateLocations.filter {
            it.timestampEpochMs >= earliestStationary.timestampEpochMs
        }.size

        if (stationaryDurationMs >= policy.prolongedStopThresholdMs && pointsInWindow >= policy.minObservationsForStop) {
            val durationMinutes = (stationaryDurationMs / 60_000L).coerceAtLeast(1)
            val severity = if (stationaryDurationMs >= policy.prolongedStopThresholdMs * 2) {
                AnomalySeverity.HIGH
            } else {
                AnomalySeverity.MEDIUM
            }

            return AnomalySignal(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                type = AnomalyType.PROLONGED_STOP,
                severity = severity,
                timestampEpochMs = currentTimeEpochMs,
                explanation = "User remained stationary within ${policy.stationaryRadiusMeters.toInt()}m for approx. $durationMinutes min.",
                confidence = 0.95f
            )
        }

        return null
    }

    /**
     * Signal 2: ROUTE_DEVIATION
     * Triggers when the latest accurate coordinate deviates outside the route corridor.
     */
    private fun detectRouteDeviation(
        sessionId: String,
        locations: List<UserLocation>,
        expectedRoute: ExpectedRoute?,
        currentTimeEpochMs: Long
    ): AnomalySignal? {
        if (expectedRoute == null || expectedRoute.waypoints.size < 2) return null
        if (locations.isEmpty()) return null

        val latest = locations.last()
        // Discard inaccurate points to avoid false-positive deviation alerts
        if (latest.accuracyMeters != null && latest.accuracyMeters > policy.maxAcceptableAccuracyMeters) {
            return null
        }

        val corridorLimit = if (expectedRoute.corridorRadiusMeters > 0) {
            expectedRoute.corridorRadiusMeters
        } else {
            policy.routeDeviationThresholdMeters
        }

        val deviationMeters = GeoMath.distanceToPolylineMeters(
            pointLat = latest.latitude,
            pointLon = latest.longitude,
            waypoints = expectedRoute.waypoints
        )

        if (deviationMeters > corridorLimit) {
            val severity = if (deviationMeters > corridorLimit * 2.5) {
                AnomalySeverity.HIGH
            } else {
                AnomalySeverity.MEDIUM
            }

            return AnomalySignal(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                type = AnomalyType.ROUTE_DEVIATION,
                severity = severity,
                timestampEpochMs = currentTimeEpochMs,
                explanation = "Current location is ${deviationMeters.toInt()}m outside expected route corridor (${corridorLimit.toInt()}m limit).",
                confidence = 0.90f
            )
        }

        return null
    }

    /**
     * Signal 3: DURATION_ANOMALY
     * Triggers when elapsed journey time exceeds expected duration multiplied by threshold.
     */
    private fun detectDurationAnomaly(
        session: JourneySession,
        expectedDurationMs: Long?,
        currentTimeEpochMs: Long
    ): AnomalySignal? {
        if (expectedDurationMs == null || expectedDurationMs <= 0) return null

        val elapsedMs = (session.endedAtEpochMs ?: currentTimeEpochMs) - session.startedAtEpochMs
        val thresholdMs = (expectedDurationMs * policy.expectedDurationMultiplier).toLong()

        if (elapsedMs > thresholdMs) {
            val actualMinutes = (elapsedMs / 60_000L).coerceAtLeast(1)
            val expectedMinutes = (expectedDurationMs / 60_000L).coerceAtLeast(1)

            val severity = if (elapsedMs > expectedDurationMs * 2.5) {
                AnomalySeverity.MEDIUM
            } else {
                AnomalySeverity.LOW
            }

            return AnomalySignal(
                id = UUID.randomUUID().toString(),
                sessionId = session.sessionId,
                type = AnomalyType.DURATION_ANOMALY,
                severity = severity,
                timestampEpochMs = currentTimeEpochMs,
                explanation = "Journey duration ($actualMinutes min) exceeds planned time ($expectedMinutes min).",
                confidence = 0.85f
            )
        }

        return null
    }

    /**
     * Signal 4: UNUSUAL_MOVEMENT
     * Triggers when the user exhibits erratic oscillation / repeated heading reversals within a short window.
     */
    private fun detectUnusualMovement(
        sessionId: String,
        locations: List<UserLocation>,
        currentTimeEpochMs: Long
    ): AnomalySignal? {
        val accurateLocations = locations.filter {
            it.accuracyMeters == null || it.accuracyMeters <= policy.maxAcceptableAccuracyMeters
        }

        if (accurateLocations.size < 4) return null

        // Evaluate the recent window
        val window = accurateLocations.takeLast(policy.unusualMovementWindowSize)

        // Total net displacement across window
        val totalDisplacement = GeoMath.distanceBetweenMeters(
            window.first().latitude, window.first().longitude,
            window.last().latitude, window.last().longitude
        )

        // Ignore if user is stationary to prevent GPS drift from being treated as movement reversals
        if (totalDisplacement < policy.minDistanceForMovementAnalysisMeters) {
            return null
        }

        // Calculate consecutive segment bearings
        val bearings = mutableListOf<Float>()
        for (i in 0 until window.size - 1) {
            val a = window[i]
            val b = window[i + 1]
            val stepDist = GeoMath.distanceBetweenMeters(a.latitude, a.longitude, b.latitude, b.longitude)
            if (stepDist >= 5.0) { // Minimum 5m step to establish reliable heading
                bearings.add(GeoMath.bearingBetweenDegrees(a.latitude, a.longitude, b.latitude, b.longitude))
            }
        }

        if (bearings.size < 3) return null

        // Count sharp direction reversals (angle difference >= 140 degrees)
        var reversals = 0
        for (i in 0 until bearings.size - 1) {
            val angleDiff = GeoMath.angleDifferenceDegrees(bearings[i], bearings[i + 1])
            if (angleDiff >= 140f) {
                reversals++
            }
        }

        if (reversals >= policy.unusualMovementDirectionReversalsThreshold) {
            return AnomalySignal(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                type = AnomalyType.UNUSUAL_MOVEMENT,
                severity = AnomalySeverity.LOW,
                timestampEpochMs = currentTimeEpochMs,
                explanation = "Observed $reversals rapid direction reversals within recent movement window.",
                confidence = 0.80f
            )
        }

        return null
    }
}
