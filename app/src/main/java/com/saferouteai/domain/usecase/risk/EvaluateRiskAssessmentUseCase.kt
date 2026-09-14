package com.saferouteai.domain.usecase.risk

import com.saferouteai.domain.model.anomaly.AnomalySignal
import com.saferouteai.domain.model.audio.AcousticSignal
import com.saferouteai.domain.model.risk.RiskAssessment
import com.saferouteai.domain.model.session.JourneySession
import com.saferouteai.domain.risk.RiskFusionEngine

/**
 * Use case to evaluate risk assessment across multi-modal sensor signals.
 */
class EvaluateRiskAssessmentUseCase(
    private val engine: RiskFusionEngine = RiskFusionEngine()
) {
    operator fun invoke(
        session: JourneySession?,
        locationAnomalies: List<AnomalySignal>,
        acousticSignals: List<AcousticSignal>,
        currentTimeEpochMs: Long
    ): RiskAssessment = engine.evaluate(
        session = session,
        locationAnomalies = locationAnomalies,
        acousticSignals = acousticSignals,
        currentTimeEpochMs = currentTimeEpochMs
    )
}
