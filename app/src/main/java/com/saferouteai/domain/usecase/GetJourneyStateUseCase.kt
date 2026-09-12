package com.saferouteai.domain.usecase

import com.saferouteai.domain.model.JourneyState
import com.saferouteai.domain.repository.JourneyRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * UseCase to observe the current active journey state.
 */
class GetJourneyStateUseCase(
    private val journeyRepository: JourneyRepository
) {
    operator fun invoke(): StateFlow<JourneyState> = journeyRepository.journeyState
}
