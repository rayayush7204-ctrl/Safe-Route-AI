package com.saferouteai.domain.usecase.session

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.session.CheckpointPolicy
import com.saferouteai.domain.model.session.JourneySession
import com.saferouteai.domain.model.session.StartJourneyError
import com.saferouteai.domain.repository.ConsentRepository
import com.saferouteai.domain.repository.JourneySessionRepository
import com.saferouteai.domain.repository.LocationPermissionChecker
import com.saferouteai.domain.usecase.location.StartLocationTrackingUseCase
import com.saferouteai.domain.usecase.location.StopLocationTrackingUseCase
import kotlinx.coroutines.flow.first

/**
 * Initiates a Safe Journey session with a hard location gate.
 *
 * Strict Invariant:
 * The session will NEVER enter ACTIVE without both explicit location consent AND
 * runtime location permission granted. If either gate is unfulfilled, the session
 * is rejected cleanly and leaves the system in a non-active state.
 */
class StartJourneySessionUseCase(
    private val sessionRepository: JourneySessionRepository,
    private val consentRepository: ConsentRepository,
    private val permissionChecker: LocationPermissionChecker,
    private val startLocationTrackingUseCase: StartLocationTrackingUseCase,
    private val stopLocationTrackingUseCase: StopLocationTrackingUseCase
) {

    /**
     * Executes the hard-gated session start sequence.
     *
     * @return [Result.Success] with active [JourneySession], or [Result.Error] with [StartJourneyException].
     */
    suspend operator fun invoke(
        origin: String? = null,
        destination: String? = null,
        policy: CheckpointPolicy = CheckpointPolicy()
    ): Result<JourneySession> {
        // Gate 1: Check explicit user consent
        val consent = consentRepository.getConsent().first()
        if (!consent.locationSharingConsent) {
            return Result.Error(StartJourneyException(StartJourneyError.ConsentRequired))
        }

        // Gate 2: Check Android runtime location permission
        if (!permissionChecker.hasLocationPermission()) {
            return Result.Error(StartJourneyException(StartJourneyError.PermissionRequired))
        }

        // Gate 3: Verify state transition can begin
        val currentStatus = sessionRepository.sessionStatus.value
        if (!currentStatus.canTransitionTo(com.saferouteai.domain.model.session.SessionStatus.STARTING)) {
            return Result.Error(
                StartJourneyException(StartJourneyError.InvalidState(currentStatus))
            )
        }

        // Start location tracking via Milestone 2B dual gate
        val trackingResult = startLocationTrackingUseCase()
        if (trackingResult is Result.Error) {
            return Result.Error(trackingResult.exception)
        }

        // Initialize active session in repository
        val sessionResult = sessionRepository.startSession(origin, destination, policy)
        if (sessionResult is Result.Error) {
            // Clean up tracking on failure
            stopLocationTrackingUseCase()
            return Result.Error(sessionResult.exception)
        }

        return sessionResult
    }
}

/**
 * Exception wrapping typed [StartJourneyError] for domain [Result.Error] channels.
 */
class StartJourneyException(val error: StartJourneyError) :
    Exception("Safe journey start failed: $error")
