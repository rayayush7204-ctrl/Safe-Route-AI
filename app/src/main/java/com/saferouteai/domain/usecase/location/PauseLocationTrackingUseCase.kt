package com.saferouteai.domain.usecase.location

import com.saferouteai.core.result.Result
import com.saferouteai.domain.repository.LocationRepository

/**
 * UseCase to pause location tracking when the application leaves the foreground.
 */
class PauseLocationTrackingUseCase(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(): Result<Unit> = locationRepository.pauseTracking()
}
