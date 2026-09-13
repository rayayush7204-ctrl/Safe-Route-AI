package com.saferouteai.presentation.home

import com.saferouteai.domain.model.Journey
import com.saferouteai.domain.model.JourneyState
import com.saferouteai.domain.model.anomaly.AnomalySignal
import com.saferouteai.domain.model.anomaly.ExpectedRoute
import com.saferouteai.domain.model.location.LocationTrackingState
import com.saferouteai.domain.model.location.UserLocation
import com.saferouteai.domain.model.session.CheckpointState
import com.saferouteai.domain.model.session.JourneySession
import com.saferouteai.domain.model.session.SessionStatus

/**
 * UI State representation for the Home screen, safe journey session engine,
 * local checkpoints, and anomaly observations.
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
    val elapsedDurationMs: Long = 0L,
    // Milestone 4 Local Journey Anomaly Intelligence additions:
    val activeAnomalies: List<AnomalySignal> = emptyList(),
    val expectedRoute: ExpectedRoute? = null,
    val expectedDurationMs: Long? = null,
    // Milestone 5A Secure Audio Pipeline additions:
    val audioCaptureState: com.saferouteai.domain.model.audio.AudioCaptureState = com.saferouteai.domain.model.audio.AudioCaptureState.IDLE,
    val isAudioConsentGranted: Boolean = false,
    val isAudioPermissionGranted: Boolean = false,
    val acousticSignals: List<com.saferouteai.domain.model.audio.AcousticSignal> = emptyList(),
    val showAudioConsentRationaleDialog: Boolean = false
) {
    val isJourneyActive: Boolean
        get() = sessionStatus.isActiveSession || journeyState == JourneyState.ACTIVE

    val isJourneyCompleted: Boolean
        get() = sessionStatus == SessionStatus.COMPLETED || journeyState == JourneyState.COMPLETED

    val isJourneyCancelled: Boolean
        get() = sessionStatus == SessionStatus.CANCELLED

    val canTrackLocation: Boolean
        get() = isConsentGranted && isPermissionGranted

    val canCaptureAudio: Boolean
        get() = isAudioConsentGranted && isAudioPermissionGranted && isJourneyActive

    val isAudioCapturing: Boolean
        get() = audioCaptureState.isCapturing
}
