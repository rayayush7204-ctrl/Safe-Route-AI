package com.saferouteai.domain.model.anomaly

/**
 * Relative importance tier of a detected anomaly signal.
 *
 * Notice: This is an observation metric, NOT an emergency or danger classification.
 */
enum class AnomalySeverity {
    LOW,
    MEDIUM,
    HIGH
}
