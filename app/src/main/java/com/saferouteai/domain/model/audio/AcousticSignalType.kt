package com.saferouteai.domain.model.audio

/**
 * Categorization of local acoustic observations.
 *
 * Invariants:
 * - Observational signal categories only.
 * - SIGNAL != DANGER, SIGNAL != EMERGENCY, SIGNAL != AUTOMATIC ALERT.
 */
enum class AcousticSignalType {
    WAKE_WORD,
    DISTRESS_KEYWORD,
    VOCAL_STRESS_PATTERN
}
