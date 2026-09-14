package com.saferouteai.domain.model.risk

/**
 * Configurable policy parameters for multi-signal risk fusion.
 *
 * All scoring weights, thresholds, temporal validity windows, and aggregation rules
 * are fully customizable without changing detector/fusion logic.
 *
 * @property normalMaxScore Maximum score for [SafetyTier.NORMAL] (0..normalMaxScore).
 * @property elevatedMaxScore Maximum score for [SafetyTier.ELEVATED] (normalMaxScore+1..elevatedMaxScore).
 * @property prolongedStopWeight Contribution for [RiskFactorType.PROLONGED_STOP].
 * @property routeDeviationWeight Contribution for [RiskFactorType.ROUTE_DEVIATION].
 * @property durationAnomalyWeight Contribution for [RiskFactorType.DURATION_ANOMALY].
 * @property unusualMovementWeight Contribution for [RiskFactorType.UNUSUAL_MOVEMENT].
 * @property suddenLoudImpactWeight Contribution for [RiskFactorType.SUDDEN_LOUD_IMPACT].
 * @property screamOrShoutWeight Contribution for [RiskFactorType.SCREAM_OR_SHOUT].
 * @property persistentDistressCommotionWeight Contribution for [RiskFactorType.PERSISTENT_DISTRESS_COMMOTION].
 * @property crossSourceBonusWeight Explicit bonus applied when both location and acoustic evidence co-occur.
 * @property locationSignalValidityMs Window in ms during which location anomaly signals remain valid.
 * @property acousticSignalValidityMs Window in ms during which acoustic signals remain valid.
 * @property maxOccurrencesPerType Maximum number of factors allowed per [RiskFactorType] (defaults to 1 for deduplication).
 */
data class RiskFusionPolicy(
    val normalMaxScore: Int = 29,
    val elevatedMaxScore: Int = 59,
    val prolongedStopWeight: Int = 15,
    val routeDeviationWeight: Int = 20,
    val durationAnomalyWeight: Int = 10,
    val unusualMovementWeight: Int = 20,
    val suddenLoudImpactWeight: Int = 15,
    val screamOrShoutWeight: Int = 25,
    val persistentDistressCommotionWeight: Int = 25,
    val crossSourceBonusWeight: Int = 15,
    val locationSignalValidityMs: Long = 5 * 60 * 1000L,
    val acousticSignalValidityMs: Long = 3 * 60 * 1000L,
    val maxOccurrencesPerType: Int = 1
) {
    init {
        require(normalMaxScore >= 0) { "normalMaxScore must be non-negative" }
        require(elevatedMaxScore > normalMaxScore) { "elevatedMaxScore must be greater than normalMaxScore" }
        require(locationSignalValidityMs > 0) { "locationSignalValidityMs must be positive" }
        require(acousticSignalValidityMs > 0) { "acousticSignalValidityMs must be positive" }
        require(maxOccurrencesPerType >= 1) { "maxOccurrencesPerType must be at least 1" }
    }
}
