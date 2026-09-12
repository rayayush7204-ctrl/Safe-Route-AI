package com.saferouteai.presentation.home

import com.saferouteai.domain.model.Journey
import com.saferouteai.domain.model.JourneyState
import com.saferouteai.domain.model.location.LocationTrackingState
import com.saferouteai.domain.model.location.UserLocation

/**
 * UI State representation for the Home screen and journey tracking flow.
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
    val errorMessage: String? = null
) {
    val isJourneyActive: Boolean
        get() = journeyState == JourneyState.ACTIVE

    val isJourneyCompleted: Boolean
        get() = journeyState == JourneyState.COMPLETED

    val canTrackLocation: Boolean
        get() = isConsentGranted && isPermissionGranted
}
