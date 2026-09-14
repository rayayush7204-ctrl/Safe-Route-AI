package com.saferouteai.domain.model.risk

/**
 * Types of discrete risk factors contributing to a fused safety assessment.
 */
enum class RiskFactorType {
    PROLONGED_STOP,
    ROUTE_DEVIATION,
    DURATION_ANOMALY,
    UNUSUAL_MOVEMENT,
    SUDDEN_LOUD_IMPACT,
    SCREAM_OR_SHOUT,
    PERSISTENT_DISTRESS_COMMOTION,
    OTHER_CONTEXT
}
