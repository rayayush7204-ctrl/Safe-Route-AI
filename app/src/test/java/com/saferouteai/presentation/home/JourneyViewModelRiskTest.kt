package com.saferouteai.presentation.home

import com.saferouteai.data.repository.InMemoryAnomalyRepository
import com.saferouteai.data.repository.InMemoryConsentRepository
import com.saferouteai.data.repository.InMemoryJourneyRepository
import com.saferouteai.data.repository.InMemoryJourneySessionRepository
import com.saferouteai.domain.model.UserConsent
import com.saferouteai.domain.model.anomaly.AnomalyType
import com.saferouteai.domain.model.location.LocationPermissionStatus
import com.saferouteai.domain.model.risk.SafetyTier
import com.saferouteai.domain.risk.RiskFusionEngine
import com.saferouteai.domain.usecase.EndJourneyUseCase
import com.saferouteai.domain.usecase.GetJourneyStateUseCase
import com.saferouteai.domain.usecase.ResetJourneyUseCase
import com.saferouteai.domain.usecase.StartJourneyUseCase
import com.saferouteai.domain.usecase.anomaly.ClearAnomaliesUseCase
import com.saferouteai.domain.usecase.anomaly.GetActiveAnomaliesUseCase
import com.saferouteai.domain.repository.FakeLocationPermissionChecker
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JourneyViewModelRiskTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var clock: FakeClock
    private lateinit var journeyRepo: InMemoryJourneyRepository
    private lateinit var sessionRepo: InMemoryJourneySessionRepository
    private lateinit var consentRepo: InMemoryConsentRepository
    private lateinit var anomalyRepo: InMemoryAnomalyRepository
    private lateinit var permissionChecker: FakeLocationPermissionChecker
    private lateinit var riskFusionEngine: RiskFusionEngine
    private lateinit var viewModel: JourneyViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        clock = FakeClock(1_000_000L)
        journeyRepo = InMemoryJourneyRepository()
        sessionRepo = InMemoryJourneySessionRepository(clock = clock)
        consentRepo = InMemoryConsentRepository(initialConsent = UserConsent(locationSharingConsent = true))
        anomalyRepo = InMemoryAnomalyRepository()
        permissionChecker = FakeLocationPermissionChecker(initialStatus = LocationPermissionStatus.FINE_GRANTED)
        riskFusionEngine = RiskFusionEngine()

        viewModel = JourneyViewModel(
            startJourneyUseCase = StartJourneyUseCase(journeyRepo),
            endJourneyUseCase = EndJourneyUseCase(journeyRepo),
            getJourneyStateUseCase = GetJourneyStateUseCase(journeyRepo),
            resetJourneyUseCase = ResetJourneyUseCase(journeyRepo),
            journeySessionRepository = sessionRepo,
            clock = clock,
            consentRepository = consentRepo,
            permissionChecker = permissionChecker,
            anomalyRepository = anomalyRepo,
            getActiveAnomaliesUseCase = GetActiveAnomaliesUseCase(anomalyRepo),
            clearAnomaliesUseCase = ClearAnomaliesUseCase(anomalyRepo),
            riskFusionEngine = riskFusionEngine
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has NORMAL risk assessment with score 0`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SafetyTier.NORMAL, state.riskAssessment.tier)
        assertEquals(0, state.riskAssessment.score)
        assertTrue(state.riskAssessment.factors.isEmpty())

        collectJob.cancel()
    }

    @Test
    fun `injected location anomaly updates risk assessment during active journey`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        // Start active session
        sessionRepo.startSession(origin = "Origin", destination = "Destination")
        advanceUntilIdle()

        // Inject test location anomaly
        viewModel.injectTestLocationAnomaly(
            type = AnomalyType.PROLONGED_STOP,
            explanation = "Stationary duration exceeded test threshold."
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(15, state.riskAssessment.score)
        assertEquals(SafetyTier.NORMAL, state.riskAssessment.tier) // 15 <= 29
        assertEquals(1, state.riskAssessment.factors.size)

        // Inject another independent anomaly to elevate
        viewModel.injectTestLocationAnomaly(
            type = AnomalyType.ROUTE_DEVIATION,
            explanation = "Route deviation."
        )
        advanceUntilIdle()

        val elevatedState = viewModel.uiState.value
        assertEquals(35, elevatedState.riskAssessment.score) // 15 + 20 = 35 -> ELEVATED
        assertEquals(SafetyTier.ELEVATED, elevatedState.riskAssessment.tier)

        collectJob.cancel()
    }

    @Test
    fun `ending journey immediately neutralizes risk assessment back to NORMAL`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        sessionRepo.startSession(origin = "Origin", destination = "Destination")
        advanceUntilIdle()

        viewModel.injectTestLocationAnomaly(
            type = AnomalyType.ROUTE_DEVIATION,
            explanation = "Route deviation."
        )
        advanceUntilIdle()

        assertEquals(20, viewModel.uiState.value.riskAssessment.score)

        // End session
        sessionRepo.endSession()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SafetyTier.NORMAL, state.riskAssessment.tier)
        assertEquals(0, state.riskAssessment.score)
        assertTrue(state.riskAssessment.factors.isEmpty())

        collectJob.cancel()
    }
}
