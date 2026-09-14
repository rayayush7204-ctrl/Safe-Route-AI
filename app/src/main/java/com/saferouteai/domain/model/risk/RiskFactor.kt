package com.saferouteai.domain.model.risk

/**
 * An individual contributing factor to a fused risk assessment.
 *
 * @property type Category of the risk factor.
 * @property contribution Point contribution to the fused risk score.
 * @property explanation Human-readable factual explanation of the observed condition.
 * @property sourceSignalId Optional reference ID of the source observation signal.
 */
data class RiskFactor(
    val type: RiskFactorType,
    val contribution: Int,
    val explanation: String,
    val sourceSignalId: String? = null
)
