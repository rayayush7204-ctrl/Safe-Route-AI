package com.saferouteai.domain.usecase

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.Journey
import com.saferouteai.domain.repository.JourneyRepository

/**
 * UseCase to conclude an active safe journey.
 */
class EndJourneyUseCase(
    private val journeyRepository: JourneyRepository
) {
    suspend operator fun invoke(): Result<Journey> = journeyRepository.endJourney()
}
