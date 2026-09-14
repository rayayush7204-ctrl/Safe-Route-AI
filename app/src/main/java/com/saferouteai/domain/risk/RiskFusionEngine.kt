package com.saferouteai.domain.risk

import com.saferouteai.domain.model.anomaly.AnomalySignal
import com.saferouteai.domain.model.anomaly.AnomalyType
import com.saferouteai.domain.model.audio.AcousticSignal
import com.saferouteai.domain.model.audio.AcousticSignalType
import com.saferouteai.domain.model.risk.RiskAssessment
import com.saferouteai.domain.model.risk.RiskFactor
import com.saferouteai.domain.model.risk.RiskFactorType
import com.saferouteai.domain.model.risk.RiskFusionPolicy
import com.saferouteai.domain.model.risk.SafetyTier
import com.saferouteai.domain.model.session.JourneySession

/**
 * Pure Kotlin, deterministic multi-signal risk fusion engine.
 *
 * Invariants:
 * - Deterministic, side-effect free evaluation with zero Android, cloud, or ML dependencies.
 * - RISK != DANGER, RISK != EMERGENCY, HIGH != AUTOMATIC SOS.
 * - RiskAssessment is purely an observational awareness layer.
 * - Signals are strictly isolated to the active session and subject to temporal expiration.
 * - Deduplication ensures repeated signals of the same type do not artificially multiply risk.
 */
class RiskFusionEngine(
    private val policy: RiskFusionPolicy = RiskFusionPolicy()
) {

    /**
     * Evaluates active session state, temporal anomaly observations, and acoustic signals
     * into a fused [RiskAssessment].
     *
     * @param session Current journey session, or null if no session exists.
     * @param locationAnomalies Recent location anomaly observations.
     * @param acousticSignals Recent acoustic observations.
     * @param currentTimeEpochMs Reference timestamp for temporal validity and decay.
     */
    fun evaluate(
        session: JourneySession?,
        locationAnomalies: List<AnomalySignal>,
        acousticSignals: List<AcousticSignal>,
        currentTimeEpochMs: Long
    ): RiskAssessment {
        // 1. Session Gate: Risk is strictly evaluated only for ongoing active sessions
        if (session == null || !session.status.isActiveSession) {
            return RiskAssessment.normal(currentTimeEpochMs)
        }

        val activeSessionId = session.sessionId
        val sessionStart = session.startedAtEpochMs

        // 2. Filter Location Anomalies by Session Identity and Temporal Validity
        val validLocationAnomalies = locationAnomalies.filter { anomaly ->
            anomaly.sessionId == activeSessionId &&
                anomaly.timestampEpochMs >= (sessionStart - 2000L) &&
                (currentTimeEpochMs - anomaly.timestampEpochMs) in -5000L..policy.locationSignalValidityMs
        }

        // 3. Filter Acoustic Signals by Session Identity and Temporal Validity
        val validAcousticSignals = acousticSignals.filter { signal ->
            (signal.sessionId.isEmpty() || signal.sessionId == activeSessionId) &&
                signal.detectedAtEpochMs >= (sessionStart - 2000L) &&
                (currentTimeEpochMs - signal.detectedAtEpochMs) in -5000L..policy.acousticSignalValidityMs
        }

        // 4. Map Location Anomalies to Candidate Risk Factors
        val locationCandidates = validLocationAnomalies.mapNotNull { anomaly ->
            when (anomaly.type) {
                AnomalyType.PROLONGED_STOP -> RiskFactor(
                    type = RiskFactorType.PROLONGED_STOP,
                    contribution = policy.prolongedStopWeight,
                    explanation = anomaly.explanation.ifBlank { "Prolonged stationary stop detected." },
                    sourceSignalId = anomaly.id
                )
                AnomalyType.ROUTE_DEVIATION -> RiskFactor(
                    type = RiskFactorType.ROUTE_DEVIATION,
                    contribution = policy.routeDeviationWeight,
                    explanation = anomaly.explanation.ifBlank { "Route deviation detected." },
                    sourceSignalId = anomaly.id
                )
                AnomalyType.DURATION_ANOMALY -> RiskFactor(
                    type = RiskFactorType.DURATION_ANOMALY,
                    contribution = policy.durationAnomalyWeight,
                    explanation = anomaly.explanation.ifBlank { "Journey duration exceeded expected window." },
                    sourceSignalId = anomaly.id
                )
                AnomalyType.UNUSUAL_MOVEMENT -> RiskFactor(
                    type = RiskFactorType.UNUSUAL_MOVEMENT,
                    contribution = policy.unusualMovementWeight,
                    explanation = anomaly.explanation.ifBlank { "Unusual movement pattern detected." },
                    sourceSignalId = anomaly.id
                )
            }
        }

        // 5. Map Acoustic Signals to Candidate Risk Factors
        val acousticCandidates = validAcousticSignals.mapNotNull { signal ->
            when (signal.type) {
                AcousticSignalType.SUDDEN_LOUD_IMPACT -> RiskFactor(
                    type = RiskFactorType.SUDDEN_LOUD_IMPACT,
                    contribution = policy.suddenLoudImpactWeight,
                    explanation = signal.explanation.ifBlank { "Sudden elevated acoustic impulse detected locally." },
                    sourceSignalId = signal.id
                )
                AcousticSignalType.SCREAM_OR_SHOUT -> RiskFactor(
                    type = RiskFactorType.SCREAM_OR_SHOUT,
                    contribution = policy.screamOrShoutWeight,
                    explanation = signal.explanation.ifBlank { "Sustained elevated vocal-like acoustic energy detected locally." },
                    sourceSignalId = signal.id
                )
                AcousticSignalType.PERSISTENT_DISTRESS_COMMOTION -> RiskFactor(
                    type = RiskFactorType.PERSISTENT_DISTRESS_COMMOTION,
                    contribution = policy.persistentDistressCommotionWeight,
                    explanation = signal.explanation.ifBlank { "Repeated elevated acoustic activity detected within the recent window." },
                    sourceSignalId = signal.id
                )
                else -> null // Other signals (e.g. WAKE_WORD) do not contribute risk points
            }
        }

        // 6. Deduplicate Factors: Apply single-contribution policy per RiskFactorType
        val selectedLocationFactors = locationCandidates
            .groupBy { it.type }
            .values
            .flatMap { group -> group.take(policy.maxOccurrencesPerType) }

        val selectedAcousticFactors = acousticCandidates
            .groupBy { it.type }
            .values
            .flatMap { group -> group.take(policy.maxOccurrencesPerType) }

        val allFactors = mutableListOf<RiskFactor>()
        allFactors.addAll(selectedLocationFactors)
        allFactors.addAll(selectedAcousticFactors)

        // 7. Independent Cross-Source Synergy Rule
        // When both location anomalies and acoustic observations are actively verified,
        // add an explicit configurable correlation factor.
        val hasLocationEvidence = selectedLocationFactors.isNotEmpty()
        val hasAcousticEvidence = selectedAcousticFactors.isNotEmpty()

        if (hasLocationEvidence && hasAcousticEvidence && policy.crossSourceBonusWeight > 0) {
            allFactors.add(
                RiskFactor(
                    type = RiskFactorType.OTHER_CONTEXT,
                    contribution = policy.crossSourceBonusWeight,
                    explanation = "Multi-modal cross-source correlation (location anomaly combined with acoustic observation)."
                )
            )
        }

        // 8. Calculate Bounded Score and Assign Safety Tier
        val rawScore = allFactors.sumOf { it.contribution }
        val score = rawScore.coerceIn(0, 100)

        val tier = when {
            allFactors.isEmpty() -> SafetyTier.NORMAL
            score <= policy.normalMaxScore -> SafetyTier.NORMAL
            score <= policy.elevatedMaxScore -> SafetyTier.ELEVATED
            else -> SafetyTier.HIGH
        }

        return RiskAssessment(
            tier = tier,
            score = score,
            factors = allFactors,
            assessedAtEpochMs = currentTimeEpochMs
        )
    }
}
