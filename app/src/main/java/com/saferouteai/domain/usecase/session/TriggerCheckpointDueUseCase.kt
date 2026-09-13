package com.saferouteai.domain.usecase.session

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.session.JourneySession
import com.saferouteai.domain.repository.JourneySessionRepository

/**
 * Triggers a checkpoint due event when the scheduled timer threshold is reached.
 */
class TriggerCheckpointDueUseCase(
    private val sessionRepository: JourneySessionRepository
) {
    suspend operator fun invoke(): Result<JourneySession> {
        return sessionRepository.triggerCheckpointDue()
    }
}
