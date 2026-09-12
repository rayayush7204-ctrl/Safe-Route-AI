package com.saferouteai.presentation.home

import com.saferouteai.data.repository.InMemoryConsentRepository
import com.saferouteai.data.repository.InMemoryJourneyRepository
import com.saferouteai.data.repository.InMemoryLocationRepository
import com.saferouteai.domain.model.JourneyState
import com.saferouteai.domain.model.UserConsent
import com.saferouteai.domain.model.location.LocationPermissionStatus
import com.saferouteai.domain.model.location.LocationTrackingState
import com.saferouteai.domain.model.location.UserLocation
import com.saferouteai.domain.repository.FakeLocationPermissionChecker
import com.saferouteai.domain.usecase.EndJourneyUseCase
import com.saferouteai.domain.usecase.GetJourneyStateUseCase
import com.saferouteai.domain.usecase.ResetJourneyUseCase
import com.saferouteai.domain.usecase.StartJourneyUseCase
import com.saferouteai.domain.usecase.location.PauseLocationTrackingUseCase
import com.saferouteai.domain.usecase.location.ResumeLocationTrackingUseCase
import com.saferouteai.domain.usecase.location.StartLocationTrackingUseCase
import com.saferouteai.domain.usecase.location.StopLocationTrackingUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JourneyViewModelLocationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var journeyRepository: InMemoryJourneyRepository
    private lateinit var locationRepository: InMemoryLocationRepository
    private lateinit var consentRepository: InMemoryConsentRepository
    private lateinit var permissionChecker: FakeLocationPermissionChecker
    private lateinit var viewModel: JourneyViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        journeyRepository = InMemoryJourneyRepository()
        locationRepository = InMemoryLocationRepository()
        consentRepository = InMemoryConsentRepository(initialConsent = UserConsent(locationSharingConsent = false))
        permissionChecker = FakeLocationPermissionChecker(initialStatus = LocationPermissionStatus.DENIED)

        viewModel = JourneyViewModel(
            startJourneyUseCase = StartJourneyUseCase(journeyRepository),
            endJourneyUseCase = EndJourneyUseCase(journeyRepository),
            getJourneyStateUseCase = GetJourneyStateUseCase(journeyRepository),
            resetJourneyUseCase = ResetJourneyUseCase(journeyRepository),
            startLocationTrackingUseCase = StartLocationTrackingUseCase(locationRepository, consentRepository, permissionChecker),
            stopLocationTrackingUseCase = StopLocationTrackingUseCase(locationRepository),
            pauseLocationTrackingUseCase = PauseLocationTrackingUseCase(locationRepository),
            resumeLocationTrackingUseCase = ResumeLocationTrackingUseCase(locationRepository, consentRepository, permissionChecker),
            locationRepository = locationRepository,
            consentRepository = consentRepository,
            permissionChecker = permissionChecker
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasDualGatesClosed() = runTest(testDispatcher) {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(JourneyState.IDLE, state.journeyState)
        assertEquals(LocationTrackingState.IDLE, state.locationTrackingState)
        assertFalse(state.isConsentGranted)
        assertFalse(state.isPermissionGranted)
        assertFalse(state.canTrackLocation)
        assertNull(state.currentLocation)

        job.cancel()
    }

    @Test
    fun startJourneyClicked_whenGatesUnmet_showsPreflightDialogWithoutStarting() = runTest(testDispatcher) {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onStartJourneyClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Preflight dialog must be shown when gates are unmet", state.showPreflightDialog)
        assertEquals("Journey must remain IDLE until gates are satisfied", JourneyState.IDLE, state.journeyState)
        assertEquals("Location must remain IDLE", LocationTrackingState.IDLE, state.locationTrackingState)

        viewModel.dismissPreflightDialog()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showPreflightDialog)

        job.cancel()
    }

    @Test
    fun startJourneyClicked_whenBothGatesSatisfied_startsJourneyAndLocationTracking() = runTest(testDispatcher) {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        consentRepository.setLocationSharingConsent(true)
        permissionChecker.setStatus(LocationPermissionStatus.FINE_GRANTED)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.canTrackLocation)

        viewModel.onStartJourneyClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(JourneyState.ACTIVE, state.journeyState)
        assertEquals(LocationTrackingState.TRACKING, state.locationTrackingState)
        assertFalse(state.showPreflightDialog)

        job.cancel()
    }

    @Test
    fun locationUpdates_propagateToUiState() = runTest(testDispatcher) {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        consentRepository.setLocationSharingConsent(true)
        permissionChecker.setStatus(LocationPermissionStatus.FINE_GRANTED)
        advanceUntilIdle()

        viewModel.onStartJourneyClicked()
        advanceUntilIdle()

        val location = UserLocation(latitude = 34.0522, longitude = -118.2437, accuracyMeters = 10f)
        locationRepository.emitLocation(location)
        advanceUntilIdle()

        assertEquals(location, viewModel.uiState.value.currentLocation)

        job.cancel()
    }

    @Test
    fun appLifecycleBackgroundAndForeground_pausesAndResumesTracking() = runTest(testDispatcher) {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        consentRepository.setLocationSharingConsent(true)
        permissionChecker.setStatus(LocationPermissionStatus.FINE_GRANTED)
        advanceUntilIdle()

        viewModel.onStartJourneyClicked()
        advanceUntilIdle()
        assertEquals(LocationTrackingState.TRACKING, viewModel.uiState.value.locationTrackingState)

        // Activity leaves foreground
        viewModel.onAppBackgrounded()
        advanceUntilIdle()
        assertEquals(LocationTrackingState.PAUSED, viewModel.uiState.value.locationTrackingState)

        // Activity returns to foreground
        viewModel.onAppForegrounded()
        advanceUntilIdle()
        assertEquals(LocationTrackingState.TRACKING, viewModel.uiState.value.locationTrackingState)

        job.cancel()
    }

    @Test
    fun endJourney_stopsLocationAndClearsVolatileCoordinates() = runTest(testDispatcher) {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        consentRepository.setLocationSharingConsent(true)
        permissionChecker.setStatus(LocationPermissionStatus.FINE_GRANTED)
        advanceUntilIdle()

        viewModel.onStartJourneyClicked()
        advanceUntilIdle()

        locationRepository.emitLocation(UserLocation(latitude = 34.0, longitude = -118.0))
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.currentLocation)

        viewModel.endJourney()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(JourneyState.COMPLETED, state.journeyState)
        assertEquals(LocationTrackingState.STOPPED, state.locationTrackingState)
        assertNull("Volatile coordinates must be cleared when journey ends", state.currentLocation)

        job.cancel()
    }
}
