package com.saferouteai.presentation.home

import com.saferouteai.domain.model.Journey
import com.saferouteai.domain.model.JourneyState

/**
 * UI State representation for the Home screen.
 */
data class JourneyUiState(
    val journeyState: JourneyState = JourneyState.IDLE,
    val currentJourney: Journey? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val isJourneyActive: Boolean
        get() = journeyState == JourneyState.ACTIVE

    val isJourneyCompleted: Boolean
        get() = journeyState == JourneyState.COMPLETED
}
