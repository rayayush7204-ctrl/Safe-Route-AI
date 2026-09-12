package com.saferouteai.domain.usecase

import com.saferouteai.core.result.Result
import com.saferouteai.domain.repository.JourneyRepository

/**
 * UseCase to reset the journey state back to IDLE.
 */
class ResetJourneyUseCase(
    private val journeyRepository: JourneyRepository
) {
    suspend operator fun invoke(): Result<Unit> = journeyRepository.resetToIdle()
}
