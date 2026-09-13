package com.saferouteai.domain.usecase.anomaly

import com.saferouteai.domain.anomaly.JourneyAnomalyDetector
import com.saferouteai.domain.model.anomaly.AnomalyDetectionPolicy
import com.saferouteai.domain.model.anomaly.AnomalySignal
import com.saferouteai.domain.model.anomaly.ExpectedRoute
import com.saferouteai.domain.model.location.UserLocation
import com.saferouteai.domain.model.session.JourneySession
import com.saferouteai.domain.repository.AnomalyRepository
import com.saferouteai.domain.time.Clock

/**
 * Evaluates the active safe journey session and recent location observations against
 * deterministic anomaly detection rules, updating the in-memory anomaly repository.
 */
class EvaluateJourneyAnomaliesUseCase(
    private val detector: JourneyAnomalyDetector,
    private val anomalyRepository: AnomalyRepository,
    private val clock: Clock
) {

    operator fun invoke(
        session: JourneySession,
        newLocation: UserLocation? = null,
        expectedRoute: ExpectedRoute? = null,
        expectedDurationMs: Long? = null,
        policy: AnomalyDetectionPolicy = AnomalyDetectionPolicy()
    ): List<AnomalySignal> {
        if (!session.status.isActiveSession) {
            anomalyRepository.clear()
            return emptyList()
        }

        if (newLocation != null) {
            anomalyRepository.addLocationObservation(newLocation)
        }

        val recentLocations = anomalyRepository.recentLocations.value
        val signals = detector.detectAnomalies(
            session = session,
            recentLocations = recentLocations,
            expectedRoute = expectedRoute,
            expectedDurationMs = expectedDurationMs,
            currentTimeEpochMs = clock.nowEpochMs()
        )

        anomalyRepository.updateSignals(signals)
        return signals
    }
}
