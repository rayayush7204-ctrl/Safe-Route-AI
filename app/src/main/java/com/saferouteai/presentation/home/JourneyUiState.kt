package com.saferouteai.presentation.home

import com.saferouteai.domain.model.Journey
import com.saferouteai.domain.model.JourneyState
import com.saferouteai.domain.model.location.LocationTrackingState
import com.saferouteai.domain.model.location.UserLocation
import com.saferouteai.domain.model.session.CheckpointState
import com.saferouteai.domain.model.session.JourneySession
import com.saferouteai.domain.model.session.SessionStatus

/**
 * UI State representation for the Home screen, safe journey session engine,
 * and checkpoint flow.
 */
data class JourneyUiState(
    val journeyState: JourneyState = JourneyState.IDLE,
    val currentJourney: Journey? = null,
    val locationTrackingState: LocationTrackingState = LocationTrackingState.IDLE,
    val currentLocation: UserLocation? = null,
    val isConsentGranted: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val showPreflightDialog: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // Milestone 3 Safe Journey Session additions:
    val session: JourneySession? = null,
    val sessionStatus: SessionStatus = SessionStatus.IDLE,
    val checkpointState: CheckpointState = CheckpointState.DISARMED,
    val nextCheckpointRemainingMs: Long? = null,
    val showSafetyCheckInDialog: Boolean = false,
    val elapsedDurationMs: Long = 0L
) {
    val isJourneyActive: Boolean
        get() = sessionStatus.isActiveSession || journeyState == JourneyState.ACTIVE

    val isJourneyCompleted: Boolean
        get() = sessionStatus == SessionStatus.COMPLETED || journeyState == JourneyState.COMPLETED

    val isJourneyCancelled: Boolean
        get() = sessionStatus == SessionStatus.CANCELLED

    val canTrackLocation: Boolean
        get() = isConsentGranted && isPermissionGranted
}
