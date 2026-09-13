package com.saferouteai.domain.usecase.anomaly

import com.saferouteai.data.repository.InMemoryAnomalyRepository
import com.saferouteai.domain.anomaly.JourneyAnomalyDetector
import com.saferouteai.domain.model.anomaly.AnomalyDetectionPolicy
import com.saferouteai.domain.model.anomaly.AnomalyType
import com.saferouteai.domain.model.location.UserLocation
import com.saferouteai.domain.model.session.JourneySession
import com.saferouteai.domain.model.session.SessionStatus
import com.saferouteai.test.FakeClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EvaluateJourneyAnomaliesUseCaseTest {

    private lateinit var clock: FakeClock
    private lateinit var anomalyRepository: InMemoryAnomalyRepository
    private lateinit var detector: JourneyAnomalyDetector
    private lateinit var evaluateUseCase: EvaluateJourneyAnomaliesUseCase
    private lateinit var activeSession: JourneySession

    @Before
    fun setUp() {
        clock = FakeClock(1_000_000L)
        anomalyRepository = InMemoryAnomalyRepository()
        detector = JourneyAnomalyDetector(
            AnomalyDetectionPolicy(
                prolongedStopThresholdMs = 300_000L, // 5 min
                stationaryRadiusMeters = 30.0,
                minObservationsForStop = 2
            )
        )
        evaluateUseCase = EvaluateJourneyAnomaliesUseCase(detector, anomalyRepository, clock)
        activeSession = JourneySession(
            sessionId = "session-1",
            status = SessionStatus.ACTIVE,
            startedAtEpochMs = 1_000_000L
        )
    }

    @Test
    fun `adding locations during active session updates repository and detects prolonged stop`() {
        // First observation at t = 1_000_000L
        val loc1 = UserLocation(37.7749, -122.4194, timestampEpochMs = 1_000_000L, accuracyMeters = 5f)
        evaluateUseCase(activeSession, newLocation = loc1)

        assertEquals(1, anomalyRepository.recentLocations.value.size)
        assertTrue(anomalyRepository.activeSignals.value.isEmpty())

        // Second observation at t = 1_400_000L (400s later, exceeds 300s threshold)
        clock.advanceTimeBy(400_000L)
        val loc2 = UserLocation(37.77491, -122.41941, timestampEpochMs = 1_400_000L, accuracyMeters = 5f)
        val signals = evaluateUseCase(activeSession, newLocation = loc2)

        assertEquals(2, anomalyRepository.recentLocations.value.size)
        assertEquals(1, signals.size)
        assertEquals(AnomalyType.PROLONGED_STOP, signals.first().type)
        assertEquals(signals, anomalyRepository.activeSignals.value)
    }

    @Test
    fun `evaluating non-active session clears repository and emits nothing`() {
        // Pre-populate with observations
        evaluateUseCase(activeSession, newLocation = UserLocation(37.7749, -122.4194, timestampEpochMs = 1_000_000L))
        assertEquals(1, anomalyRepository.recentLocations.value.size)

        // Now session is completed
        val completedSession = activeSession.copy(status = SessionStatus.COMPLETED)
        val signals = evaluateUseCase(completedSession)

        assertTrue(signals.isEmpty())
        assertTrue(anomalyRepository.recentLocations.value.isEmpty())
        assertTrue(anomalyRepository.activeSignals.value.isEmpty())
    }
}
