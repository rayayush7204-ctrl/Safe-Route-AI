package com.saferouteai.domain.model.anomaly

/**
 * Categories of local journey anomalies detected during an active Safe Journey session.
 */
enum class AnomalyType {
    /**
     * User has remained stationary within a narrow radius beyond the configured duration threshold.
     */
    PROLONGED_STOP,

    /**
     * Current location deviates materially outside the expected route corridor.
     */
    ROUTE_DEVIATION,

    /**
     * Active journey duration has materially exceeded the expected travel time.
     */
    DURATION_ANOMALY,

    /**
     * Erratic or unusual movement sequence (e.g. repeated directional reversals).
     */
    UNUSUAL_MOVEMENT
}
