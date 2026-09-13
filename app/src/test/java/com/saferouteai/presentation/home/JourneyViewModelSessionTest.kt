package com.saferouteai.presentation.home

import com.saferouteai.data.repository.InMemoryConsentRepository
import com.saferouteai.data.repository.InMemoryJourneyRepository
import com.saferouteai.data.repository.InMemoryJourneySessionRepository
import com.saferouteai.data.repository.InMemoryLocationRepository
import com.saferouteai.domain.model.UserConsent
import com.saferouteai.domain.model.location.LocationPermissionStatus
import com.saferouteai.domain.model.location.LocationTrackingState
import com.saferouteai.domain.model.session.CheckpointPolicy
import com.saferouteai.domain.model.session.CheckpointState
import com.saferouteai.domain.model.session.SessionStatus
import com.saferouteai.domain.repository.FakeLocationPermissionChecker
import com.saferouteai.domain.usecase.EndJourneyUseCase
import com.saferouteai.domain.usecase.GetJourneyStateUseCase
import com.saferouteai.domain.usecase.ResetJourneyUseCase
import com.saferouteai.domain.usecase.StartJourneyUseCase
import com.saferouteai.domain.usecase.location.StartLocationTrackingUseCase
import com.saferouteai.domain.usecase.location.StopLocationTrackingUseCase
import com.saferouteai.domain.usecase.session.AcknowledgeCheckpointUseCase
import com.saferouteai.domain.usecase.session.CancelJourneySessionUseCase
import com.saferouteai.domain.usecase.session.EndJourneySessionUseCase
import com.saferouteai.domain.usecase.session.ResetJourneySessionUseCase
import com.saferouteai.domain.usecase.session.StartJourneySessionUseCase
import com.saferouteai.test.FakeClock
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JourneyViewModelSessionTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var clock: FakeClock
    private lateinit var journeyRepository: InMemoryJourneyRepository
    private lateinit var sessionRepository: InMemoryJourneySessionRepository
    private lateinit var locationRepository: InMemoryLocationRepository
    private lateinit var consentRepository: InMemoryConsentRepository
    private lateinit var permissionChecker: FakeLocationPermissionChecker

    private lateinit var viewModel: JourneyViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        clock = FakeClock(1_000_000L)
        journeyRepository = InMemoryJourneyRepository()
        sessionRepository = InMemoryJourneySessionRepository(clock)
        locationRepository = InMemoryLocationRepository()
        consentRepository = InMemoryConsentRepository(initialConsent = UserConsent(locationSharingConsent = false))
        permissionChecker = FakeLocationPermissionChecker(initialStatus = LocationPermissionStatus.DENIED)

        val startTrackingUseCase = StartLocationTrackingUseCase(locationRepository, consentRepository, permissionChecker)
        val stopTrackingUseCase = StopLocationTrackingUseCase(locationRepository)

        val startSessionUseCase = StartJourneySessionUseCase(
            sessionRepository = sessionRepository,
            consentRepository = consentRepository,
            permissionChecker = permissionChecker,
            startLocationTrackingUseCase = startTrackingUseCase,
            stopLocationTrackingUseCase = stopTrackingUseCase
        )
        val endSessionUseCase = EndJourneySessionUseCase(sessionRepository, stopTrackingUseCase)
        val cancelSessionUseCase = CancelJourneySessionUseCase(sessionRepository, stopTrackingUseCase)
        val ackCheckpointUseCase = AcknowledgeCheckpointUseCase(sessionRepository)
        val resetSessionUseCase = ResetJourneySessionUseCase(sessionRepository)

        viewModel = JourneyViewModel(
            startJourneyUseCase = StartJourneyUseCase(journeyRepository),
            endJourneyUseCase = EndJourneyUseCase(journeyRepository),
            getJourneyStateUseCase = GetJourneyStateUseCase(journeyRepository),
            resetJourneyUseCase = ResetJourneyUseCase(journeyRepository),
            startLocationTrackingUseCase = startTrackingUseCase,
            stopLocationTrackingUseCase = stopTrackingUseCase,
            locationRepository = locationRepository,
            consentRepository = consentRepository,
            permissionChecker = permissionChecker,
            startJourneySessionUseCase = startSessionUseCase,
            endJourneySessionUseCase = endSessionUseCase,
            cancelJourneySessionUseCase = cancelSessionUseCase,
            acknowledgeCheckpointUseCase = ackCheckpointUseCase,
            resetJourneySessionUseCase = resetSessionUseCase,
            journeySessionRepository = sessionRepository,
            clock = clock
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is idle and checkpoints are disarmed`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SessionStatus.IDLE, state.sessionStatus)
        assertEquals(CheckpointState.DISARMED, state.checkpointState)
        assertFalse(state.isJourneyActive)
        assertFalse(state.showSafetyCheckInDialog)
        assertFalse(state.showPreflightDialog)

        collectJob.cancel()
    }

    @Test
    fun `starting journey without consent triggers preflight dialog`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onStartJourneyClicked()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showPreflightDialog)
        assertEquals(SessionStatus.IDLE, viewModel.uiState.value.sessionStatus)

        collectJob.cancel()
    }

    @Test
    fun `starting journey without permission triggers preflight dialog`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        consentRepository.updateConsent(UserConsent(locationSharingConsent = true))
        advanceUntilIdle()

        viewModel.onStartJourneyClicked()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showPreflightDialog)
        assertEquals(SessionStatus.IDLE, viewModel.uiState.value.sessionStatus)

        collectJob.cancel()
    }

    @Test
    fun `starting journey with both gates satisfied transitions to active session`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        consentRepository.updateConsent(UserConsent(locationSharingConsent = true))
        permissionChecker.setStatus(LocationPermissionStatus.FINE_GRANTED)
        advanceUntilIdle()

        viewModel.onStartJourneyClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isJourneyActive)
        assertEquals(SessionStatus.ACTIVE, state.sessionStatus)
        assertEquals(LocationTrackingState.TRACKING, state.locationTrackingState)
        assertEquals(CheckpointState.SCHEDULED, state.checkpointState)
        assertNotNull(state.nextCheckpointRemainingMs)

        collectJob.cancel()
    }

    @Test
    fun `triggering checkpoint due displays safety check-in dialog`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        consentRepository.updateConsent(UserConsent(locationSharingConsent = true))
        permissionChecker.setStatus(LocationPermissionStatus.FINE_GRANTED)
        advanceUntilIdle()

        viewModel.onStartJourneyClicked()
        advanceUntilIdle()

        // Advance time to checkpoint
        clock.advanceTimeBy(CheckpointPolicy.DEFAULT_INTERVAL_MS)
        sessionRepository.triggerCheckpointDue()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SessionStatus.CHECKPOINT_DUE, state.sessionStatus)
        assertEquals(CheckpointState.DUE, state.checkpointState)
        assertTrue(state.showSafetyCheckInDialog)

        collectJob.cancel()
    }

    @Test
    fun `acknowledging checkpoint hides dialog and reschedules checkpoint`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        consentRepository.updateConsent(UserConsent(locationSharingConsent = true))
        permissionChecker.setStatus(LocationPermissionStatus.FINE_GRANTED)
        advanceUntilIdle()

        viewModel.onStartJourneyClicked()
        advanceUntilIdle()

        clock.advanceTimeBy(CheckpointPolicy.DEFAULT_INTERVAL_MS)
        sessionRepository.triggerCheckpointDue()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showSafetyCheckInDialog)

        viewModel.onAcknowledgeCheckpoint()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showSafetyCheckInDialog)
        assertEquals(SessionStatus.ACTIVE, state.sessionStatus)
        assertEquals(CheckpointState.SCHEDULED, state.checkpointState)

        collectJob.cancel()
    }

    @Test
    fun `endJourney stops tracking and completes session`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        consentRepository.updateConsent(UserConsent(locationSharingConsent = true))
        permissionChecker.setStatus(LocationPermissionStatus.FINE_GRANTED)
        advanceUntilIdle()

        viewModel.onStartJourneyClicked()
        advanceUntilIdle()

        viewModel.endJourney()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isJourneyCompleted)
        assertEquals(SessionStatus.COMPLETED, state.sessionStatus)
        assertEquals(LocationTrackingState.STOPPED, state.locationTrackingState)

        collectJob.cancel()
    }

    @Test
    fun `cancelJourney stops tracking and cancels session`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        consentRepository.updateConsent(UserConsent(locationSharingConsent = true))
        permissionChecker.setStatus(LocationPermissionStatus.FINE_GRANTED)
        advanceUntilIdle()

        viewModel.onStartJourneyClicked()
        advanceUntilIdle()

        viewModel.cancelJourney()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isJourneyCancelled)
        assertEquals(SessionStatus.CANCELLED, state.sessionStatus)
        assertEquals(LocationTrackingState.STOPPED, state.locationTrackingState)

        collectJob.cancel()
    }

    @Test
    fun `resetJourney resets terminal state back to IDLE`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        consentRepository.updateConsent(UserConsent(locationSharingConsent = true))
        permissionChecker.setStatus(LocationPermissionStatus.FINE_GRANTED)
        advanceUntilIdle()

        viewModel.onStartJourneyClicked()
        advanceUntilIdle()
        viewModel.endJourney()
        advanceUntilIdle()

        viewModel.resetJourney()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SessionStatus.IDLE, state.sessionStatus)
        assertFalse(state.isJourneyActive)
        assertFalse(state.isJourneyCompleted)

        collectJob.cancel()
    }
}
