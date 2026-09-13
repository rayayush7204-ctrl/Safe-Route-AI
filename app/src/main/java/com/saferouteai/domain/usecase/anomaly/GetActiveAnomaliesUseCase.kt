package com.saferouteai.domain.usecase.anomaly

import com.saferouteai.domain.model.anomaly.AnomalySignal
import com.saferouteai.domain.repository.AnomalyRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Observes the in-memory stream of active anomaly signals.
 */
class GetActiveAnomaliesUseCase(
    private val anomalyRepository: AnomalyRepository
) {
    operator fun invoke(): StateFlow<List<AnomalySignal>> = anomalyRepository.activeSignals
}
