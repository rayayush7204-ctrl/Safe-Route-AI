package com.saferouteai.domain.usecase.location

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.location.LocationError
import com.saferouteai.domain.repository.ConsentRepository
import com.saferouteai.domain.repository.LocationPermissionChecker
import com.saferouteai.domain.repository.LocationRepository
import kotlinx.coroutines.flow.first

/**
 * UseCase to initiate foreground location tracking.
 *
 * Enforces the dual-gate invariant:
 * 1. locationSharingConsent MUST be true.
 * 2. Android runtime location permission MUST be granted.
 * Tracking cannot start if either requirement is unmet.
 */
class StartLocationTrackingUseCase(
    private val locationRepository: LocationRepository,
    private val consentRepository: ConsentRepository,
    private val permissionChecker: LocationPermissionChecker
) {
    suspend operator fun invoke(): Result<Unit> {
        val consent = consentRepository.getConsent().first()
        if (!consent.locationSharingConsent) {
            return Result.Error(IllegalStateException(LocationError.ConsentNotGranted().message))
        }

        if (!permissionChecker.hasLocationPermission()) {
            return Result.Error(IllegalStateException(LocationError.PermissionDenied().message))
        }

        return locationRepository.startTracking()
    }
}
