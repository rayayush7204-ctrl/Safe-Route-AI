package com.saferouteai.domain.usecase.anomaly

import com.saferouteai.domain.repository.AnomalyRepository

/**
 * Clears active anomaly signals and in-memory observation buffers.
 */
class ClearAnomaliesUseCase(
    private val anomalyRepository: AnomalyRepository
) {
    operator fun invoke() {
        anomalyRepository.clear()
    }
}
