package com.saferouteai.domain.usecase.session

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.session.JourneySession
import com.saferouteai.domain.repository.JourneySessionRepository
import com.saferouteai.domain.usecase.location.StopLocationTrackingUseCase

/**
 * Cancels an active or starting Safe Journey session and halts location tracking.
 */
class CancelJourneySessionUseCase(
    private val sessionRepository: JourneySessionRepository,
    private val stopLocationTrackingUseCase: StopLocationTrackingUseCase
) {
    suspend operator fun invoke(): Result<JourneySession> {
        stopLocationTrackingUseCase()
        return sessionRepository.cancelSession()
    }
}
