package com.saferouteai.domain.usecase.session

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.session.JourneySession
import com.saferouteai.domain.repository.JourneySessionRepository

/**
 * Acknowledges a due safety checkpoint ("I'm Okay") in the foreground UI.
 */
class AcknowledgeCheckpointUseCase(
    private val sessionRepository: JourneySessionRepository
) {
    suspend operator fun invoke(): Result<JourneySession> {
        return sessionRepository.acknowledgeCheckpoint()
    }
}
