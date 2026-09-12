package com.saferouteai.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.JourneyState
import com.saferouteai.domain.model.location.LocationPermissionStatus
import com.saferouteai.domain.model.location.LocationTrackingState
import com.saferouteai.domain.model.location.UserLocation
import com.saferouteai.domain.repository.ConsentRepository
import com.saferouteai.domain.repository.JourneyRepository
import com.saferouteai.domain.repository.LocationPermissionChecker
import com.saferouteai.domain.repository.LocationRepository
import com.saferouteai.domain.usecase.EndJourneyUseCase
import com.saferouteai.domain.usecase.GetJourneyStateUseCase
import com.saferouteai.domain.usecase.ResetJourneyUseCase
import com.saferouteai.domain.usecase.StartJourneyUseCase
import com.saferouteai.domain.usecase.location.PauseLocationTrackingUseCase
import com.saferouteai.domain.usecase.location.ResumeLocationTrackingUseCase
import com.saferouteai.domain.usecase.location.StartLocationTrackingUseCase
import com.saferouteai.domain.usecase.location.StopLocationTrackingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing the UI state and user interactions for the SafeRoute home journey flow.
 * Coordinates Journey lifecycle and dual-gated foreground Location tracking.
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
    private val permissionChecker: LocationPermissionChecker? = null
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _showPreflightDialog = MutableStateFlow(false)

    private val consentFlow = consentRepository?.getConsent()?.let { flow ->
        combine(flow) { it[0] }
    } ?: flowOf(null)

    private val permissionStatusFlow = permissionChecker?.permissionStatus ?: flowOf(LocationPermissionStatus.NOT_REQUESTED)
    private val trackingStateFlow = locationRepository?.trackingState ?: flowOf(LocationTrackingState.IDLE)
    private val currentLocationFlow = locationRepository?.currentLocation ?: flowOf(null)

    val uiState: StateFlow<JourneyUiState> = combine(
        getJourneyStateUseCase(),
        trackingStateFlow,
        currentLocationFlow,
        consentFlow,
        permissionStatusFlow,
        _showPreflightDialog,
        _isLoading,
        _errorMessage
    ) { args: Array<Any?> ->
        val journeyState = args[0] as JourneyState
        val trackingState = args[1] as LocationTrackingState
        val currentLocation = args[2] as? UserLocation
        val consent = args[3] as? com.saferouteai.domain.model.UserConsent
        val permissionStatus = args[4] as LocationPermissionStatus
        val showPreflight = args[5] as Boolean
        val isLoading = args[6] as Boolean
        val errorMessage = args[7] as? String

        val isConsentGranted = consent?.locationSharingConsent ?: false
        val isPermissionGranted = permissionStatus.isGranted

        JourneyUiState(
            journeyState = journeyState,
            locationTrackingState = trackingState,
            currentLocation = currentLocation,
            isConsentGranted = isConsentGranted,
            isPermissionGranted = isPermissionGranted,
            showPreflightDialog = showPreflight,
            isLoading = isLoading,
            errorMessage = errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = JourneyUiState()
    )

    fun onStartJourneyClicked() {
        val currentState = uiState.value
        if (!currentState.canTrackLocation) {
            _showPreflightDialog.value = true
            return
        }
        startJourneyAndTracking()
    }

    /**
     * Backward-compatible programmatic start method.
     * Starts tracking if use case is provided and gates are met, or directly starts journey if in legacy mode.
     */
    fun startJourney() {
        if (startLocationTrackingUseCase == null) {
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
            _isLoading.value = false
        }
    }

    fun endJourney() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // Stop location tracking and clear volatile coordinates
            stopLocationTrackingUseCase?.invoke()

            when (val result = endJourneyUseCase()) {
                is Result.Success -> {
                    // Journey completed
                }
                is Result.Error -> {
                    _errorMessage.value = result.exception.localizedMessage ?: "Failed to end journey"
                }
            }
            _isLoading.value = false
        }
    }

    fun resetJourney() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            stopLocationTrackingUseCase?.invoke()
            resetJourneyUseCase?.invoke()
            _isLoading.value = false
        }
    }

    /**
     * Activity / UI Lifecycle: App transitioned to background.
     * Pauses foreground location tracking to ensure zero background tracking.
     */
    fun onAppBackgrounded() {
        if (uiState.value.isJourneyActive) {
            viewModelScope.launch {
                pauseLocationTrackingUseCase?.invoke()
            }
        }
    }

    /**
     * Activity / UI Lifecycle: App returned to foreground.
     * Resumes foreground tracking if journey remains active and gates are satisfied.
     */
    fun onAppForegrounded() {
        if (uiState.value.isJourneyActive) {
            viewModelScope.launch {
                resumeLocationTrackingUseCase?.invoke()
            }
        }
    }

    fun dismissPreflightDialog() {
        _showPreflightDialog.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        fun provideFactory(
            journeyRepository: JourneyRepository,
            locationRepository: LocationRepository? = null,
            consentRepository: ConsentRepository? = null,
            permissionChecker: LocationPermissionChecker? = null
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
                    permissionChecker = permissionChecker
                ) as T
            }
        }
    }
}
