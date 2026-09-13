package com.saferouteai.domain.usecase.session

import com.saferouteai.domain.model.session.JourneySession
import com.saferouteai.domain.repository.JourneySessionRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Observes the currently active or latest Safe Journey session.
 */
class GetActiveJourneySessionUseCase(
    private val sessionRepository: JourneySessionRepository
) {
    operator fun invoke(): StateFlow<JourneySession?> = sessionRepository.activeSession
}
