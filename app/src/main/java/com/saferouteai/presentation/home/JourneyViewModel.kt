package com.saferouteai.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.saferouteai.core.result.Result
import com.saferouteai.data.time.SystemClock
import com.saferouteai.domain.anomaly.JourneyAnomalyDetector
import com.saferouteai.domain.model.JourneyState
import com.saferouteai.domain.model.anomaly.AnomalySignal
import com.saferouteai.domain.model.anomaly.ExpectedRoute
import com.saferouteai.domain.model.audio.AudioCaptureState
import com.saferouteai.domain.model.audio.AudioPermissionStatus
import com.saferouteai.domain.model.location.LocationPermissionStatus
import com.saferouteai.domain.model.location.LocationTrackingState
import com.saferouteai.domain.model.location.UserLocation
import com.saferouteai.domain.model.session.CheckpointPolicy
import com.saferouteai.domain.model.session.CheckpointState
import com.saferouteai.domain.model.session.JourneySession
import com.saferouteai.domain.model.session.SessionStatus
import com.saferouteai.domain.model.session.StartJourneyError
import com.saferouteai.domain.repository.AnomalyRepository
import com.saferouteai.domain.repository.AudioPermissionChecker
import com.saferouteai.domain.repository.AudioRepository
import com.saferouteai.domain.repository.ConsentRepository
import com.saferouteai.domain.repository.JourneyRepository
import com.saferouteai.domain.repository.JourneySessionRepository
import com.saferouteai.domain.repository.LocationPermissionChecker
import com.saferouteai.domain.repository.LocationRepository
import com.saferouteai.domain.time.Clock
import com.saferouteai.domain.usecase.EndJourneyUseCase
import com.saferouteai.domain.usecase.GetJourneyStateUseCase
import com.saferouteai.domain.usecase.ResetJourneyUseCase
import com.saferouteai.domain.usecase.StartJourneyUseCase
import com.saferouteai.domain.usecase.anomaly.ClearAnomaliesUseCase
import com.saferouteai.domain.usecase.anomaly.EvaluateJourneyAnomaliesUseCase
import com.saferouteai.domain.usecase.anomaly.GetActiveAnomaliesUseCase
import com.saferouteai.domain.usecase.location.PauseLocationTrackingUseCase
import com.saferouteai.domain.usecase.location.ResumeLocationTrackingUseCase
import com.saferouteai.domain.usecase.location.StartLocationTrackingUseCase
import com.saferouteai.domain.usecase.location.StopLocationTrackingUseCase
import com.saferouteai.domain.usecase.session.AcknowledgeCheckpointUseCase
import com.saferouteai.domain.usecase.session.CancelJourneySessionUseCase
import com.saferouteai.domain.usecase.session.EndJourneySessionUseCase
import com.saferouteai.domain.usecase.session.ResetJourneySessionUseCase
import com.saferouteai.domain.usecase.session.StartJourneyException
import com.saferouteai.domain.usecase.session.StartJourneySessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing the UI state and user interactions for the SafeRoute home journey flow.
 * Coordinates Journey lifecycle, dual-gated foreground Location tracking, Milestone 3
 * Safe Journey Session Engine with local periodic checkpoints, and Milestone 4 Local Journey
 * Anomaly Intelligence.
 */
class JourneyViewModel(
    private val startJourneyUseCase: StartJourneyUseCase,
    private val endJourneyUseCase: EndJourneyUseCase,
    private val getJourneyStateUseCase: GetJourneyStateUseCase,
    private val resetJourneyUseCase: ResetJourneyUseCase? = null,
    private val startLocationTrackingUseCase: StartLocationTrackingUseCase? = null,
    private val stopLocationTrackingUseCase: StopLocationTrackingUseCase? = null,
    private val pauseLocationTrackingUseCase: PauseLocationTrackingUseCase? = null,
    private val resumeLocationTrackingUseCase: ResumeLocationTrackingUseCase? = null,
    private val locationRepository: LocationRepository? = null,
    private val consentRepository: ConsentRepository? = null,
    private val permissionChecker: LocationPermissionChecker? = null,
    // Milestone 3 Session Engine additions:
    private val startJourneySessionUseCase: StartJourneySessionUseCase? = null,
    private val endJourneySessionUseCase: EndJourneySessionUseCase? = null,
    private val cancelJourneySessionUseCase: CancelJourneySessionUseCase? = null,
    private val acknowledgeCheckpointUseCase: AcknowledgeCheckpointUseCase? = null,
    private val resetJourneySessionUseCase: ResetJourneySessionUseCase? = null,
    private val journeySessionRepository: JourneySessionRepository? = null,
    private val clock: Clock = SystemClock(),
    // Milestone 4 Anomaly Intelligence additions:
    private val evaluateJourneyAnomaliesUseCase: EvaluateJourneyAnomaliesUseCase? = null,
    private val getActiveAnomaliesUseCase: GetActiveAnomaliesUseCase? = null,
    private val clearAnomaliesUseCase: ClearAnomaliesUseCase? = null,
    // Milestone 5A Secure Audio Pipeline additions:
    private val audioRepository: AudioRepository? = null,
    private val audioPermissionChecker: AudioPermissionChecker? = null
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _showPreflightDialog = MutableStateFlow(false)
    private val _showSafetyCheckInDialog = MutableStateFlow(false)
    private val _expectedRoute = MutableStateFlow<ExpectedRoute?>(null)
    private val _expectedDurationMs = MutableStateFlow<Long?>(null)

    private val consentFlow = consentRepository?.getConsent()?.let { flow ->
        combine(flow) { it[0] }
    } ?: flowOf(null)

    private val permissionStatusFlow = permissionChecker?.permissionStatus ?: flowOf(LocationPermissionStatus.NOT_REQUESTED)
    private val trackingStateFlow = locationRepository?.trackingState ?: flowOf(LocationTrackingState.IDLE)
    private val currentLocationFlow = locationRepository?.currentLocation ?: flowOf(null)
    private val activeSessionFlow = journeySessionRepository?.activeSession ?: flowOf(null)
    private val sessionStatusFlow = journeySessionRepository?.sessionStatus ?: flowOf(SessionStatus.IDLE)
    private val activeAnomaliesFlow = getActiveAnomaliesUseCase?.invoke() ?: flowOf(emptyList())
    private val audioCaptureStateFlow = audioRepository?.captureState ?: flowOf(AudioCaptureState.IDLE)
    private val audioPermissionFlow = audioPermissionChecker?.permissionStatus ?: flowOf(AudioPermissionStatus.NOT_REQUESTED)
    private val acousticSignalsFlow = audioRepository?.acousticSignals ?: flowOf(emptyList())

    val uiState: StateFlow<JourneyUiState> = combine(
        getJourneyStateUseCase(),
        trackingStateFlow,
        currentLocationFlow,
        consentFlow,
        permissionStatusFlow,
        activeSessionFlow,
        sessionStatusFlow,
        activeAnomaliesFlow,
        _expectedRoute,
        _expectedDurationMs,
        _showPreflightDialog,
        _showSafetyCheckInDialog,
        _isLoading,
        _errorMessage,
        audioCaptureStateFlow,
        audioPermissionFlow,
        acousticSignalsFlow
    ) { args: Array<Any?> ->
        val journeyState = args[0] as JourneyState
        val trackingState = args[1] as LocationTrackingState
        val currentLocation = args[2] as? UserLocation
        val consent = args[3] as? com.saferouteai.domain.model.UserConsent
        val permissionStatus = args[4] as LocationPermissionStatus
        val session = args[5] as? JourneySession
        val sessionStatus = args[6] as SessionStatus
        @Suppress("UNCHECKED_CAST")
        val activeAnomalies = args[7] as List<AnomalySignal>
        val expectedRoute = args[8] as? ExpectedRoute
        val expectedDurationMs = args[9] as? Long
        val showPreflight = args[10] as Boolean
        val showCheckInDialog = args[11] as Boolean
        val isLoading = args[12] as Boolean
        val errorMessage = args[13] as? String
        val audioCaptureState = args[14] as AudioCaptureState
        val audioPermStatus = args[15] as AudioPermissionStatus
        @Suppress("UNCHECKED_CAST")
        val acousticSignals = args[16] as List<com.saferouteai.domain.model.audio.AcousticSignal>

        val isConsentGranted = consent?.locationSharingConsent ?: false
        val isPermissionGranted = permissionStatus.isGranted
        val isAudioConsentGranted = consent?.audioProcessingConsent ?: false
        val isAudioPermissionGranted = audioPermStatus.isGranted

        val elapsedDurationMs = session?.calculateDurationMs(clock) ?: 0L
        val nextCheckpointRemainingMs = session?.nextCheckpointEpochMs?.let { next ->
            (next - clock.nowEpochMs()).coerceAtLeast(0L)
        }

        // Auto-show check-in dialog when status is CHECKPOINT_DUE
        val effectiveShowCheckIn = showCheckInDialog || sessionStatus == SessionStatus.CHECKPOINT_DUE

        JourneyUiState(
            journeyState = journeyState,
            locationTrackingState = trackingState,
            currentLocation = currentLocation ?: session?.latestLocation,
            isConsentGranted = isConsentGranted,
            isPermissionGranted = isPermissionGranted,
            showPreflightDialog = showPreflight,
            isLoading = isLoading,
            errorMessage = errorMessage,
            session = session,
            sessionStatus = sessionStatus,
            checkpointState = session?.checkpointState ?: CheckpointState.DISARMED,
            nextCheckpointRemainingMs = nextCheckpointRemainingMs,
            showSafetyCheckInDialog = effectiveShowCheckIn,
            elapsedDurationMs = elapsedDurationMs,
            activeAnomalies = activeAnomalies,
            expectedRoute = expectedRoute,
            expectedDurationMs = expectedDurationMs,
            audioCaptureState = audioCaptureState,
            isAudioConsentGranted = isAudioConsentGranted,
            isAudioPermissionGranted = isAudioPermissionGranted,
            acousticSignals = acousticSignals
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = JourneyUiState()
    )

    init {
        // Evaluate anomalies automatically whenever a new location coordinate arrives during active session
        if (locationRepository != null && evaluateJourneyAnomaliesUseCase != null) {
            viewModelScope.launch {
                locationRepository.currentLocation.collectLatest { location ->
                    val session = journeySessionRepository?.activeSession?.value ?: uiState.value.session
                    if (session != null && session.status.isActiveSession && location != null) {
                        evaluateJourneyAnomaliesUseCase(
                            session = session,
                            newLocation = location,
                            expectedRoute = _expectedRoute.value,
                            expectedDurationMs = _expectedDurationMs.value
                        )
                    }
                }
            }
        }
    }

    fun onStartJourneyClicked() {
        val currentState = uiState.value
        // Hard start gate: if location prerequisites are missing, trigger preflight or permission prompt
        if (!currentState.canTrackLocation) {
            _showPreflightDialog.value = true
            return
        }
        startJourneyAndTracking()
    }

    /**
     * Backward-compatible programmatic start method.
     */
    fun startJourney() {
        if (startLocationTrackingUseCase == null && startJourneySessionUseCase == null) {
            startJourneyAndTracking()
        } else {
            onStartJourneyClicked()
        }
    }

    fun onPermissionResult(status: LocationPermissionStatus) {
        if (status.isGranted && uiState.value.isConsentGranted) {
            startJourneyAndTracking()
        }
    }

    private fun startJourneyAndTracking() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _showPreflightDialog.value = false

            // Hard start gate via Milestone 3 Session Engine if configured
            if (startJourneySessionUseCase != null) {
                when (val result = startJourneySessionUseCase(policy = CheckpointPolicy())) {
                    is Result.Success -> {
                        // Also sync legacy journey repository state for backward compatibility
                        startJourneyUseCase()
                        // Initial anomaly evaluation
                        evaluateJourneyAnomaliesUseCase?.invoke(
                            session = result.data,
                            expectedRoute = _expectedRoute.value,
                            expectedDurationMs = _expectedDurationMs.value
                        )
                    }
                    is Result.Error -> {
                        val ex = result.exception
                        if (ex is StartJourneyException) {
                            when (ex.error) {
                                StartJourneyError.ConsentRequired -> _showPreflightDialog.value = true
                                StartJourneyError.PermissionRequired -> _showPreflightDialog.value = true
                                is StartJourneyError.InvalidState -> _errorMessage.value = "Cannot start journey: ${ex.error.currentStatus}"
                                is StartJourneyError.StorageError -> _errorMessage.value = ex.error.message
                            }
                        } else {
                            _errorMessage.value = ex.localizedMessage ?: "Failed to start safe journey"
                        }
                    }
                }
            } else {
                // Fallback for legacy Milestone 1 / 2B tests without Session UseCase
                when (val journeyResult = startJourneyUseCase()) {
                    is Result.Success -> {
                        startLocationTrackingUseCase?.invoke()?.let { trackingResult ->
                            if (trackingResult is Result.Error) {
                                _errorMessage.value = trackingResult.exception.localizedMessage
                            }
                        }
                    }
                    is Result.Error -> {
                        _errorMessage.value = journeyResult.exception.localizedMessage ?: "Failed to start journey"
                    }
                }
            }
            _isLoading.value = false
        }
    }

    fun endJourney() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _showSafetyCheckInDialog.value = false
            clearAnomaliesUseCase?.invoke()
            audioRepository?.stopCapture()
            audioRepository?.clearSignals()

            if (endJourneySessionUseCase != null) {
                when (val result = endJourneySessionUseCase.invoke()) {
                    is Result.Success -> {
                        endJourneyUseCase()
                    }
                    is Result.Error -> {
                        _errorMessage.value = result.exception.localizedMessage ?: "Failed to end journey"
                    }
                }
            } else {
                stopLocationTrackingUseCase?.invoke()
                when (val result = endJourneyUseCase()) {
                    is Result.Success -> { /* Completed */ }
                    is Result.Error -> {
                        _errorMessage.value = result.exception.localizedMessage ?: "Failed to end journey"
                    }
                }
            }
            _isLoading.value = false
        }
    }

    fun cancelJourney() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _showSafetyCheckInDialog.value = false
            clearAnomaliesUseCase?.invoke()
            audioRepository?.stopCapture()
            audioRepository?.clearSignals()

            if (cancelJourneySessionUseCase != null) {
                when (val result = cancelJourneySessionUseCase.invoke()) {
                    is Result.Success -> {
                        endJourneyUseCase()
                    }
                    is Result.Error -> {
                        _errorMessage.value = result.exception.localizedMessage ?: "Failed to cancel journey"
                    }
                }
            } else {
                stopLocationTrackingUseCase?.invoke()
                endJourneyUseCase()
            }
            _isLoading.value = false
        }
    }

    fun onAcknowledgeCheckpoint() {
        viewModelScope.launch {
            _showSafetyCheckInDialog.value = false
            acknowledgeCheckpointUseCase?.invoke()
        }
    }

    fun resetJourney() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _showSafetyCheckInDialog.value = false
            clearAnomaliesUseCase?.invoke()
            audioRepository?.stopCapture()
            audioRepository?.clearSignals()
            stopLocationTrackingUseCase?.invoke()
            resetJourneySessionUseCase?.invoke()
            resetJourneyUseCase?.invoke()
            _isLoading.value = false
        }
    }

    fun setExpectedRoute(route: ExpectedRoute?) {
        _expectedRoute.value = route
    }

    fun setExpectedDuration(durationMs: Long?) {
        _expectedDurationMs.value = durationMs
    }

    fun evaluateAnomalies() {
        val session = uiState.value.session ?: return
        evaluateJourneyAnomaliesUseCase?.invoke(
            session = session,
            expectedRoute = _expectedRoute.value,
            expectedDurationMs = _expectedDurationMs.value
        )
    }

    fun dismissSafetyCheckInDialog() {
        _showSafetyCheckInDialog.value = false
    }

    fun dismissPreflightDialog() {
        _showPreflightDialog.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Starts audio capture if all gates (active session + audio consent + RECORD_AUDIO permission) are met.
     */
    fun startAudioCapture() {
        viewModelScope.launch {
            audioRepository?.startCapture()
        }
    }

    /**
     * Stops audio capture and clears volatile signals.
     */
    fun stopAudioCapture() {
        viewModelScope.launch {
            audioRepository?.stopCapture()
        }
    }

    /**
     * Handles the result of the Android RECORD_AUDIO runtime permission request.
     * Auto-starts audio capture if all gates are now satisfied.
     */
    fun onAudioPermissionResult(status: AudioPermissionStatus) {
        if (status.isGranted && uiState.value.isJourneyActive && uiState.value.isAudioConsentGranted) {
            startAudioCapture()
        }
    }

    /**
     * Activity / UI Lifecycle: App transitioned to background.
     * Pauses foreground location tracking and stops audio capture to ensure zero background capture.
     */
    fun onAppBackgrounded() {
        if (uiState.value.isJourneyActive) {
            viewModelScope.launch {
                pauseLocationTrackingUseCase?.invoke()
                audioRepository?.stopCapture()
            }
        }
    }

    /**
     * Activity / UI Lifecycle: App returned to foreground.
     * Resumes foreground tracking and audio capture if journey remains active and gates are satisfied.
     */
    fun onAppForegrounded() {
        if (uiState.value.isJourneyActive) {
            viewModelScope.launch {
                resumeLocationTrackingUseCase?.invoke()
                if (uiState.value.canCaptureAudio) {
                    audioRepository?.startCapture()
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            journeyRepository: JourneyRepository,
            locationRepository: LocationRepository? = null,
            consentRepository: ConsentRepository? = null,
            permissionChecker: LocationPermissionChecker? = null,
            journeySessionRepository: JourneySessionRepository? = null,
            clock: Clock = SystemClock(),
            anomalyRepository: AnomalyRepository? = null,
            anomalyDetector: JourneyAnomalyDetector? = null,
            audioRepository: AudioRepository? = null,
            audioPermissionChecker: AudioPermissionChecker? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val startTrackingUseCase = if (locationRepository != null && consentRepository != null && permissionChecker != null) {
                    StartLocationTrackingUseCase(locationRepository, consentRepository, permissionChecker)
                } else null

                val stopTrackingUseCase = locationRepository?.let { StopLocationTrackingUseCase(it) }
                val pauseTrackingUseCase = locationRepository?.let { PauseLocationTrackingUseCase(it) }
                val resumeTrackingUseCase = if (locationRepository != null && consentRepository != null && permissionChecker != null) {
                    ResumeLocationTrackingUseCase(locationRepository, consentRepository, permissionChecker)
                } else null

                val startSessionUseCase = if (journeySessionRepository != null && consentRepository != null && permissionChecker != null && startTrackingUseCase != null && stopTrackingUseCase != null) {
                    StartJourneySessionUseCase(
                        sessionRepository = journeySessionRepository,
                        consentRepository = consentRepository,
                        permissionChecker = permissionChecker,
                        startLocationTrackingUseCase = startTrackingUseCase,
                        stopLocationTrackingUseCase = stopTrackingUseCase
                    )
                } else null

                val endSessionUseCase = if (journeySessionRepository != null && stopTrackingUseCase != null) {
                    EndJourneySessionUseCase(journeySessionRepository, stopTrackingUseCase)
                } else null

                val cancelSessionUseCase = if (journeySessionRepository != null && stopTrackingUseCase != null) {
                    CancelJourneySessionUseCase(journeySessionRepository, stopTrackingUseCase)
                } else null

                val ackCheckpointUseCase = journeySessionRepository?.let { AcknowledgeCheckpointUseCase(it) }
                val resetSessionUseCase = journeySessionRepository?.let { ResetJourneySessionUseCase(it) }

                val effectiveDetector = anomalyDetector ?: JourneyAnomalyDetector()
                val evaluateAnomaliesUseCase = if (anomalyRepository != null) {
                    EvaluateJourneyAnomaliesUseCase(effectiveDetector, anomalyRepository, clock)
                } else null
                val getAnomaliesUseCase = anomalyRepository?.let { GetActiveAnomaliesUseCase(it) }
                val clearAnomaliesUseCase = anomalyRepository?.let { ClearAnomaliesUseCase(it) }

                return JourneyViewModel(
                    startJourneyUseCase = StartJourneyUseCase(journeyRepository),
                    endJourneyUseCase = EndJourneyUseCase(journeyRepository),
                    getJourneyStateUseCase = GetJourneyStateUseCase(journeyRepository),
                    resetJourneyUseCase = ResetJourneyUseCase(journeyRepository),
                    startLocationTrackingUseCase = startTrackingUseCase,
                    stopLocationTrackingUseCase = stopTrackingUseCase,
                    pauseLocationTrackingUseCase = pauseTrackingUseCase,
                    resumeLocationTrackingUseCase = resumeTrackingUseCase,
                    locationRepository = locationRepository,
                    consentRepository = consentRepository,
                    permissionChecker = permissionChecker,
                    startJourneySessionUseCase = startSessionUseCase,
                    endJourneySessionUseCase = endSessionUseCase,
                    cancelJourneySessionUseCase = cancelSessionUseCase,
                    acknowledgeCheckpointUseCase = ackCheckpointUseCase,
                    resetJourneySessionUseCase = resetSessionUseCase,
                    journeySessionRepository = journeySessionRepository,
                    clock = clock,
                    evaluateJourneyAnomaliesUseCase = evaluateAnomaliesUseCase,
                    getActiveAnomaliesUseCase = getAnomaliesUseCase,
                    clearAnomaliesUseCase = clearAnomaliesUseCase,
                    audioRepository = audioRepository,
                    audioPermissionChecker = audioPermissionChecker
                ) as T
            }
        }
    }
}
