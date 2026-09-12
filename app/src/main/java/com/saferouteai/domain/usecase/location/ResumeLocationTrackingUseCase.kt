package com.saferouteai.domain.usecase.location

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.location.LocationError
import com.saferouteai.domain.repository.ConsentRepository
import com.saferouteai.domain.repository.LocationPermissionChecker
import com.saferouteai.domain.repository.LocationRepository
import kotlinx.coroutines.flow.first

/**
 * UseCase to resume location tracking when returning to the foreground.
 *
 * Re-validates that both consent and runtime permission remain satisfied.
 */
class ResumeLocationTrackingUseCase(
    private val locationRepository: LocationRepository,
    private val consentRepository: ConsentRepository,
    private val permissionChecker: LocationPermissionChecker
) {
    suspend operator fun invoke(): Result<Unit> {
        val consent = consentRepository.getConsent().first()
        if (!consent.locationSharingConsent) {
            locationRepository.stopTracking()
            return Result.Error(IllegalStateException(LocationError.ConsentNotGranted().message))
        }

        if (!permissionChecker.hasLocationPermission()) {
            locationRepository.stopTracking()
            return Result.Error(IllegalStateException(LocationError.PermissionDenied().message))
        }

        return locationRepository.resumeTracking()
    }
}
