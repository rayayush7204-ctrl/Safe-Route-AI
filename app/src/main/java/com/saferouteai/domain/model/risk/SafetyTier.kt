package com.saferouteai.domain.model.risk

/**
 * Categorization of overall journey safety status.
 *
 * Invariants:
 * - Observational risk level only.
 * - RISK != DANGER, RISK != EMERGENCY, HIGH != AUTOMATIC SOS.
 */
enum class SafetyTier {
    /**
     * Standard baseline status with no significant active risk observations.
     */
    NORMAL,

    /**
     * One or more low-to-moderate heuristic signals observed.
     */
    ELEVATED,

    /**
     * Multiple independent or high-significance heuristic signals observed.
     */
    HIGH
}
