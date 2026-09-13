package com.saferouteai.presentation.home

import com.saferouteai.data.repository.InMemoryAnomalyRepository
import com.saferouteai.data.repository.InMemoryConsentRepository
import com.saferouteai.data.repository.InMemoryJourneyRepository
import com.saferouteai.data.repository.InMemoryJourneySessionRepository
import com.saferouteai.data.repository.InMemoryLocationRepository
import com.saferouteai.domain.anomaly.JourneyAnomalyDetector
import com.saferouteai.domain.model.JourneyState
import com.saferouteai.domain.model.UserConsent
import com.saferouteai.domain.model.anomaly.AnomalyDetectionPolicy
import com.saferouteai.domain.model.anomaly.AnomalyType
import com.saferouteai.domain.model.location.LocationPermissionStatus
import com.saferouteai.domain.model.location.UserLocation
import com.saferouteai.domain.repository.FakeLocationPermissionChecker
import com.saferouteai.domain.usecase.EndJourneyUseCase
import com.saferouteai.domain.usecase.GetJourneyStateUseCase
import com.saferouteai.domain.usecase.ResetJourneyUseCase
import com.saferouteai.domain.usecase.StartJourneyUseCase
import com.saferouteai.domain.usecase.anomaly.ClearAnomaliesUseCase
import com.saferouteai.domain.usecase.anomaly.EvaluateJourneyAnomaliesUseCase
import com.saferouteai.domain.usecase.anomaly.GetActiveAnomaliesUseCase
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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class JourneyViewModelAnomalyTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var clock: FakeClock
    private lateinit var journeyRepository: InMemoryJourneyRepository
    private lateinit var sessionRepository: InMemoryJourneySessionRepository
    private lateinit var locationRepository: InMemoryLocationRepository
    private lateinit var consentRepository: InMemoryConsentRepository
    private lateinit var permissionChecker: FakeLocationPermissionChecker
    private lateinit var anomalyRepository: InMemoryAnomalyRepository
    private lateinit var anomalyDetector: JourneyAnomalyDetector

    private lateinit var viewModel: JourneyViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        clock = FakeClock(1_000_000L)
        journeyRepository = InMemoryJourneyRepository()
        sessionRepository = InMemoryJourneySessionRepository(clock)
        locationRepository = InMemoryLocationRepository()
        consentRepository = InMemoryConsentRepository(initialConsent = UserConsent(locationSharingConsent = true))
        permissionChecker = FakeLocationPermissionChecker(initialStatus = LocationPermissionStatus.FINE_GRANTED)
        anomalyRepository = InMemoryAnomalyRepository()
        anomalyDetector = JourneyAnomalyDetector(
            AnomalyDetectionPolicy(
                prolongedStopThresholdMs = 300_000L, // 5 min
                stationaryRadiusMeters = 30.0,
                minObservationsForStop = 2
            )
        )

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

        val evaluateAnomaliesUseCase = EvaluateJourneyAnomaliesUseCase(anomalyDetector, anomalyRepository, clock)
        val getAnomaliesUseCase = GetActiveAnomaliesUseCase(anomalyRepository)
        val clearAnomaliesUseCase = ClearAnomaliesUseCase(anomalyRepository)

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
            clock = clock,
            evaluateJourneyAnomaliesUseCase = evaluateAnomaliesUseCase,
            getActiveAnomaliesUseCase = getAnomaliesUseCase,
            clearAnomaliesUseCase = clearAnomaliesUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `anomalies are empty initially`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.activeAnomalies.isEmpty())

        collectJob.cancel()
    }

    @Test
    fun `detected anomaly propagates to uiState activeAnomalies`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onStartJourneyClicked()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isJourneyActive)

        // Simulate 2 stationary location updates spanning 400s (> 300s threshold)
        locationRepository.emitLocation(
            UserLocation(37.7749, -122.4194, timestampEpochMs = 1_000_000L, accuracyMeters = 5f)
        )
        advanceUntilIdle()

        clock.advanceTimeBy(400_000L)
        locationRepository.emitLocation(
            UserLocation(37.77491, -122.41941, timestampEpochMs = 1_400_000L, accuracyMeters = 5f)
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.activeAnomalies.size)
        assertEquals(AnomalyType.PROLONGED_STOP, state.activeAnomalies.first().type)

        collectJob.cancel()
    }

    @Test
    fun `ending journey clears active anomalies`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onStartJourneyClicked()
        advanceUntilIdle()

        locationRepository.emitLocation(UserLocation(37.7749, -122.4194, timestampEpochMs = 1_000_000L, accuracyMeters = 5f))
        advanceUntilIdle()
        clock.advanceTimeBy(400_000L)
        locationRepository.emitLocation(UserLocation(37.77491, -122.41941, timestampEpochMs = 1_400_000L, accuracyMeters = 5f))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.activeAnomalies.size)

        // End journey
        viewModel.endJourney()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.activeAnomalies.isEmpty())
        assertFalse(viewModel.uiState.value.isJourneyActive)

        collectJob.cancel()
    }

    @Test
    fun `cancelling journey clears active anomalies`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onStartJourneyClicked()
        advanceUntilIdle()

        locationRepository.emitLocation(UserLocation(37.7749, -122.4194, timestampEpochMs = 1_000_000L, accuracyMeters = 5f))
        advanceUntilIdle()
        clock.advanceTimeBy(400_000L)
        locationRepository.emitLocation(UserLocation(37.77491, -122.41941, timestampEpochMs = 1_400_000L, accuracyMeters = 5f))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.activeAnomalies.size)

        // Cancel journey
        viewModel.cancelJourney()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.activeAnomalies.isEmpty())
        assertTrue(viewModel.uiState.value.isJourneyCancelled)

        collectJob.cancel()
    }

    @Test
    fun `viewModel construction succeeds under unconfined dispatcher without NPE and initial state is valid`() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val vm = JourneyViewModel(
                startJourneyUseCase = StartJourneyUseCase(journeyRepository),
                endJourneyUseCase = EndJourneyUseCase(journeyRepository),
                getJourneyStateUseCase = GetJourneyStateUseCase(journeyRepository),
                resetJourneyUseCase = ResetJourneyUseCase(journeyRepository),
                startLocationTrackingUseCase = StartLocationTrackingUseCase(locationRepository, consentRepository, permissionChecker),
                stopLocationTrackingUseCase = StopLocationTrackingUseCase(locationRepository),
                locationRepository = locationRepository,
                consentRepository = consentRepository,
                permissionChecker = permissionChecker,
                startJourneySessionUseCase = StartJourneySessionUseCase(
                    sessionRepository = sessionRepository,
                    consentRepository = consentRepository,
                    permissionChecker = permissionChecker,
                    startLocationTrackingUseCase = StartLocationTrackingUseCase(locationRepository, consentRepository, permissionChecker),
                    stopLocationTrackingUseCase = StopLocationTrackingUseCase(locationRepository)
                ),
                endJourneySessionUseCase = EndJourneySessionUseCase(sessionRepository, StopLocationTrackingUseCase(locationRepository)),
                cancelJourneySessionUseCase = CancelJourneySessionUseCase(sessionRepository, StopLocationTrackingUseCase(locationRepository)),
                acknowledgeCheckpointUseCase = AcknowledgeCheckpointUseCase(sessionRepository),
                resetJourneySessionUseCase = ResetJourneySessionUseCase(sessionRepository),
                journeySessionRepository = sessionRepository,
                clock = clock,
                evaluateJourneyAnomaliesUseCase = EvaluateJourneyAnomaliesUseCase(anomalyDetector, anomalyRepository, clock),
                getActiveAnomaliesUseCase = GetActiveAnomaliesUseCase(anomalyRepository),
                clearAnomaliesUseCase = ClearAnomaliesUseCase(anomalyRepository)
            )
            val initialState = vm.uiState.value
            assertNotNull(initialState)
            assertEquals(JourneyState.IDLE, initialState.journeyState)
            assertTrue(initialState.activeAnomalies.isEmpty())
        } finally {
            Dispatchers.setMain(testDispatcher)
        }
    }

    @Test
    fun `location updates emitted before active session do not trigger anomaly evaluation or crash`() = runTest {
        locationRepository.emitLocation(UserLocation(37.7749, -122.4194, timestampEpochMs = 1_000_000L))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.activeAnomalies.isEmpty())
    }
}

