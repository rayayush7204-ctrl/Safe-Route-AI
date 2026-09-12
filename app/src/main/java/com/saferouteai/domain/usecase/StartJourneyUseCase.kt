package com.saferouteai.domain.usecase

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.Journey
import com.saferouteai.domain.repository.JourneyRepository

/**
 * UseCase to initiate a safe journey.
 */
class StartJourneyUseCase(
    private val journeyRepository: JourneyRepository
) {
    suspend operator fun invoke(): Result<Journey> = journeyRepository.startJourney()
}
