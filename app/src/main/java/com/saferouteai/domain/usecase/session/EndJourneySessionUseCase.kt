package com.saferouteai.domain.usecase.session

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.session.JourneySession
import com.saferouteai.domain.repository.JourneySessionRepository
import com.saferouteai.domain.usecase.location.StopLocationTrackingUseCase

/**
 * Concludes an active Safe Journey session and halts location tracking.
 */
class EndJourneySessionUseCase(
    private val sessionRepository: JourneySessionRepository,
    private val stopLocationTrackingUseCase: StopLocationTrackingUseCase
) {
    suspend operator fun invoke(): Result<JourneySession> {
        // Stop foreground location tracking
        stopLocationTrackingUseCase()

        // Transition session to COMPLETED in repository
        return sessionRepository.endSession()
    }
}
