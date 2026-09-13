package com.saferouteai.domain.model.anomaly

/**
 * Configurable thresholds and tuning parameters for the local anomaly detection engine.
 *
 * All parameters are fully documented and adjustable without altering detection algorithms.
 *
 * @property prolongedStopThresholdMs Minimum time in milliseconds the user must remain within
 * [stationaryRadiusMeters] to trigger a PROLONGED_STOP observation (default: 10 mins).
 * @property stationaryRadiusMeters Geodesic radius in meters absorbing typical urban GPS drift (default: 35.0m).
 * @property routeDeviationThresholdMeters Permissible lateral distance in meters outside the route corridor (default: 100.0m).
 * @property expectedDurationMultiplier Multiplier on expected journey duration before triggering DURATION_ANOMALY (default: 1.5x).
 * @property maxAcceptableAccuracyMeters Accuracy ceiling; observations with accuracy worse than this threshold are ignored to prevent noisy false positives (default: 50.0m).
 * @property minObservationsForStop Minimum valid location points required across the time window before evaluating a stop (default: 3).
 * @property unusualMovementWindowSize Number of consecutive recent locations evaluated for erratic movement patterns (default: 6).
 * @property unusualMovementDirectionReversalsThreshold Number of sharp heading reversals (>140°) triggering UNUSUAL_MOVEMENT (default: 3).
 * @property minDistanceForMovementAnalysisMeters Minimum displacement required before analyzing directional changes, preventing stationary GPS jitter from triggering false movement signals (default: 15.0m).
 */
data class AnomalyDetectionPolicy(
    val prolongedStopThresholdMs: Long = DEFAULT_STOP_THRESHOLD_MS,
    val stationaryRadiusMeters: Double = DEFAULT_STATIONARY_RADIUS_METERS,
    val routeDeviationThresholdMeters: Double = DEFAULT_ROUTE_DEVIATION_METERS,
    val expectedDurationMultiplier: Double = DEFAULT_DURATION_MULTIPLIER,
    val maxAcceptableAccuracyMeters: Float = DEFAULT_MAX_ACCURACY_METERS,
    val minObservationsForStop: Int = DEFAULT_MIN_OBSERVATIONS_FOR_STOP,
    val unusualMovementWindowSize: Int = DEFAULT_MOVEMENT_WINDOW_SIZE,
    val unusualMovementDirectionReversalsThreshold: Int = DEFAULT_REVERSALS_THRESHOLD,
    val minDistanceForMovementAnalysisMeters: Double = DEFAULT_MIN_DISTANCE_MOVEMENT_METERS
) {
    init {
        require(prolongedStopThresholdMs > 0) { "prolongedStopThresholdMs must be positive" }
        require(stationaryRadiusMeters > 0) { "stationaryRadiusMeters must be positive" }
        require(routeDeviationThresholdMeters > 0) { "routeDeviationThresholdMeters must be positive" }
        require(expectedDurationMultiplier > 1.0) { "expectedDurationMultiplier must be greater than 1.0" }
        require(maxAcceptableAccuracyMeters > 0) { "maxAcceptableAccuracyMeters must be positive" }
        require(minObservationsForStop >= 2) { "minObservationsForStop must be at least 2" }
        require(unusualMovementWindowSize >= 4) { "unusualMovementWindowSize must be at least 4" }
    }

    companion object {
        const val DEFAULT_STOP_THRESHOLD_MS = 10 * 60 * 1000L // 10 minutes
        const val DEFAULT_STATIONARY_RADIUS_METERS = 35.0
        const val DEFAULT_ROUTE_DEVIATION_METERS = 100.0
        const val DEFAULT_DURATION_MULTIPLIER = 1.5
        const val DEFAULT_MAX_ACCURACY_METERS = 50.0f
        const val DEFAULT_MIN_OBSERVATIONS_FOR_STOP = 3
        const val DEFAULT_MOVEMENT_WINDOW_SIZE = 6
        const val DEFAULT_REVERSALS_THRESHOLD = 3
        const val DEFAULT_MIN_DISTANCE_MOVEMENT_METERS = 15.0
    }
}
