package com.saferouteai.domain.usecase.location

import com.saferouteai.core.result.Result
import com.saferouteai.domain.repository.LocationRepository

/**
 * UseCase to terminate location tracking and release all hardware resources.
 */
class StopLocationTrackingUseCase(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(): Result<Unit> = locationRepository.stopTracking()
}
