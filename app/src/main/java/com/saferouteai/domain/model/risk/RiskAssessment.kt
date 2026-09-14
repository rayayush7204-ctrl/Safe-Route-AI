package com.saferouteai.domain.model.risk

/**
 * Deterministic, fused multi-signal safety assessment for an active journey.
 *
 * Invariants:
 * - Held strictly in volatile memory.
 * - Does NOT trigger automated emergency actions, SOS, or cloud dispatch.
 * - Explanations are strictly factual and observational.
 *
 * @property tier Overall categorized tier (NORMAL, ELEVATED, HIGH).
 * @property score Aggregated risk score bounded to [0, 100].
 * @property factors Contributing factors explaining the score.
 * @property assessedAtEpochMs Timestamp when the assessment was computed.
 */
data class RiskAssessment(
    val tier: SafetyTier,
    val score: Int,
    val factors: List<RiskFactor>,
    val assessedAtEpochMs: Long
) {
    companion object {
        fun normal(assessedAtEpochMs: Long = 0L): RiskAssessment = RiskAssessment(
            tier = SafetyTier.NORMAL,
            score = 0,
            factors = emptyList(),
            assessedAtEpochMs = assessedAtEpochMs
        )
    }
}
