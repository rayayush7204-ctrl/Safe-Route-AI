package com.saferouteai.domain.usecase.session

import com.saferouteai.core.result.Result
import com.saferouteai.domain.repository.JourneySessionRepository

/**
 * Resets the session state machine from COMPLETED or CANCELLED back to IDLE.
 */
class ResetJourneySessionUseCase(
    private val sessionRepository: JourneySessionRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return sessionRepository.resetToIdle()
    }
}
