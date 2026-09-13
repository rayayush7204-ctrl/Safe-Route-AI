package com.saferouteai.domain.usecase.session

import com.saferouteai.core.result.Result
import com.saferouteai.data.repository.InMemoryConsentRepository
import com.saferouteai.data.repository.InMemoryJourneySessionRepository
import com.saferouteai.data.repository.InMemoryLocationRepository
import com.saferouteai.domain.model.UserConsent
import com.saferouteai.domain.model.location.LocationPermissionStatus
import com.saferouteai.domain.model.location.LocationTrackingState
import com.saferouteai.domain.model.session.CheckpointPolicy
import com.saferouteai.domain.model.session.CheckpointState
import com.saferouteai.domain.model.session.SessionStatus
import com.saferouteai.domain.model.session.StartJourneyError
import com.saferouteai.domain.repository.FakeLocationPermissionChecker
import com.saferouteai.domain.usecase.location.StartLocationTrackingUseCase
import com.saferouteai.domain.usecase.location.StopLocationTrackingUseCase
import com.saferouteai.test.FakeClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JourneySessionUseCasesTest {

    private lateinit var clock: FakeClock
    private lateinit var sessionRepository: InMemoryJourneySessionRepository
    private lateinit var locationRepository: InMemoryLocationRepository
    private lateinit var consentRepository: InMemoryConsentRepository
    private lateinit var permissionChecker: FakeLocationPermissionChecker

    private lateinit var startLocationTrackingUseCase: StartLocationTrackingUseCase
    private lateinit var stopLocationTrackingUseCase: StopLocationTrackingUseCase

    private lateinit var startJourneySessionUseCase: StartJourneySessionUseCase
    private lateinit var endJourneySessionUseCase: EndJourneySessionUseCase
    private lateinit var cancelJourneySessionUseCase: CancelJourneySessionUseCase
    private lateinit var acknowledgeCheckpointUseCase: AcknowledgeCheckpointUseCase
    private lateinit var triggerCheckpointDueUseCase: TriggerCheckpointDueUseCase
    private lateinit var resetJourneySessionUseCase: ResetJourneySessionUseCase

    @Before
    fun setUp() {
        clock = FakeClock(1_000_000L)
        sessionRepository = InMemoryJourneySessionRepository(clock)
        locationRepository = InMemoryLocationRepository()
        consentRepository = InMemoryConsentRepository(
            initialConsent = UserConsent(locationSharingConsent = false)
        )
        permissionChecker = FakeLocationPermissionChecker(
            initialStatus = LocationPermissionStatus.DENIED
        )

        startLocationTrackingUseCase = StartLocationTrackingUseCase(
            locationRepository = locationRepository,
            consentRepository = consentRepository,
            permissionChecker = permissionChecker
        )
        stopLocationTrackingUseCase = StopLocationTrackingUseCase(locationRepository)

        startJourneySessionUseCase = StartJourneySessionUseCase(
            sessionRepository = sessionRepository,
            consentRepository = consentRepository,
            permissionChecker = permissionChecker,
            startLocationTrackingUseCase = startLocationTrackingUseCase,
            stopLocationTrackingUseCase = stopLocationTrackingUseCase
        )
        endJourneySessionUseCase = EndJourneySessionUseCase(
            sessionRepository = sessionRepository,
            stopLocationTrackingUseCase = stopLocationTrackingUseCase
        )
        cancelJourneySessionUseCase = CancelJourneySessionUseCase(
            sessionRepository = sessionRepository,
            stopLocationTrackingUseCase = stopLocationTrackingUseCase
        )
        acknowledgeCheckpointUseCase = AcknowledgeCheckpointUseCase(sessionRepository)
        triggerCheckpointDueUseCase = TriggerCheckpointDueUseCase(sessionRepository)
        resetJourneySessionUseCase = ResetJourneySessionUseCase(sessionRepository)
    }

    @Test
    fun `session start is blocked when location consent is false`() = runTest {
        consentRepository.updateConsent(UserConsent(locationSharingConsent = false))
        permissionChecker.setStatus(LocationPermissionStatus.FINE_GRANTED)

        val result = startJourneySessionUseCase()
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).exception
        assertTrue(error is StartJourneyException)
        assertEquals(StartJourneyError.ConsentRequired, (error as StartJourneyException).error)

        // Session must remain IDLE and location tracking must NOT be active
        assertEquals(SessionStatus.IDLE, sessionRepository.sessionStatus.value)
        assertNull(sessionRepository.activeSession.value)
        assertEquals(LocationTrackingState.IDLE, locationRepository.trackingState.value)
    }

    @Test
    fun `session start is blocked when runtime location permission is unavailable`() = runTest {
        consentRepository.updateConsent(UserConsent(locationSharingConsent = true))
        permissionChecker.setStatus(LocationPermissionStatus.DENIED)

        val result = startJourneySessionUseCase()
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).exception
        assertTrue(error is StartJourneyException)
        assertEquals(StartJourneyError.PermissionRequired, (error as StartJourneyException).error)

        // Session must remain IDLE and location tracking must NOT be active
        assertEquals(SessionStatus.IDLE, sessionRepository.sessionStatus.value)
        assertNull(sessionRepository.activeSession.value)
        assertEquals(LocationTrackingState.IDLE, locationRepository.trackingState.value)
    }

    @Test
    fun `session enters ACTIVE only after both location gates succeed`() = runTest {
        consentRepository.updateConsent(UserConsent(locationSharingConsent = true))
        permissionChecker.setStatus(LocationPermissionStatus.FINE_GRANTED)

        val policy = CheckpointPolicy(intervalMs = 900_000L) // 15 mins
        val result = startJourneySessionUseCase(
            origin = "Origin Point",
            destination = "Destination Point",
            policy = policy
        )

        assertTrue(result is Result.Success)
        val session = (result as Result.Success).data
        assertEquals(SessionStatus.ACTIVE, sessionRepository.sessionStatus.value)
        assertEquals(LocationTrackingState.TRACKING, locationRepository.trackingState.value)
        assertEquals(1_000_000L, session.startedAtEpochMs)
        assertEquals(1_900_000L, session.nextCheckpointEpochMs)
        assertEquals(CheckpointState.SCHEDULED, session.checkpointState)
    }

    @Test
    fun `fake-clock advances to checkpoint due and acknowledgement calculates next checkpoint`() = runTest {
        consentRepository.updateConsent(UserConsent(locationSharingConsent = true))
        permissionChecker.setStatus(LocationPermissionStatus.FINE_GRANTED)
        val policy = CheckpointPolicy(intervalMs = 900_000L)
        startJourneySessionUseCase(policy = policy)

        // Advance fake clock by 15 mins without real sleeping
        clock.advanceTimeBy(900_000L) // t = 1_900_000L
        val triggerResult = triggerCheckpointDueUseCase()
        assertTrue(triggerResult is Result.Success)
        assertEquals(SessionStatus.CHECKPOINT_DUE, sessionRepository.sessionStatus.value)
        assertEquals(CheckpointState.DUE, sessionRepository.activeSession.value?.checkpointState)

        // User acknowledges at t = 1_910_000L (10 seconds later)
        clock.advanceTimeBy(10_000L)
        val ackResult = acknowledgeCheckpointUseCase()
        assertTrue(ackResult is Result.Success)

        val active = sessionRepository.activeSession.value
        assertNotNull(active)
        assertEquals(SessionStatus.ACTIVE, sessionRepository.sessionStatus.value)
        assertEquals(1_910_000L, active?.lastCheckpointEpochMs)
        assertEquals(1_910_000L + 900_000L, active?.nextCheckpointEpochMs)
        assertEquals(CheckpointState.SCHEDULED, active?.checkpointState)
    }

    @Test
    fun `fake-clock detects checkpoint expiry without sleeping`() = runTest {
        consentRepository.updateConsent(UserConsent(locationSharingConsent = true))
        permissionChecker.setStatus(LocationPermissionStatus.FINE_GRANTED)
        val policy = CheckpointPolicy(
            intervalMs = 900_000L,
            acknowledgementWindowMs = 180_000L // 3 minutes
        )
        startJourneySessionUseCase(policy = policy)

        clock.advanceTimeBy(900_000L)
        triggerCheckpointDueUseCase()

        // Before expiry: 2 minutes in
        clock.advanceTimeBy(120_000L)
        sessionRepository.checkCheckpointExpiry()
        assertEquals(CheckpointState.DUE, sessionRepository.activeSession.value?.checkpointState)

        // After expiry: 2 more minutes in (total 4m > 3m window)
        clock.advanceTimeBy(120_000L)
        sessionRepository.checkCheckpointExpiry()
        assertEquals(CheckpointState.MISSED, sessionRepository.activeSession.value?.checkpointState)
    }

    @Test
    fun `endSession stops location tracking and transitions session to COMPLETED`() = runTest {
        consentRepository.updateConsent(UserConsent(locationSharingConsent = true))
        permissionChecker.setStatus(LocationPermissionStatus.FINE_GRANTED)
        startJourneySessionUseCase()

        assertEquals(LocationTrackingState.TRACKING, locationRepository.trackingState.value)

        clock.advanceTimeBy(600_000L)
        val endResult = endJourneySessionUseCase()
        assertTrue(endResult is Result.Success)

        assertEquals(SessionStatus.COMPLETED, sessionRepository.sessionStatus.value)
        assertEquals(LocationTrackingState.STOPPED, locationRepository.trackingState.value)
        assertEquals(1_600_000L, sessionRepository.activeSession.value?.endedAtEpochMs)
    }

    @Test
    fun `cancelSession stops location tracking and transitions session to CANCELLED`() = runTest {
        consentRepository.updateConsent(UserConsent(locationSharingConsent = true))
        permissionChecker.setStatus(LocationPermissionStatus.FINE_GRANTED)
        startJourneySessionUseCase()

        val cancelResult = cancelJourneySessionUseCase()
        assertTrue(cancelResult is Result.Success)

        assertEquals(SessionStatus.CANCELLED, sessionRepository.sessionStatus.value)
        assertEquals(LocationTrackingState.STOPPED, locationRepository.trackingState.value)
    }

    @Test
    fun `resetSession resets terminal state back to IDLE`() = runTest {
        consentRepository.updateConsent(UserConsent(locationSharingConsent = true))
        permissionChecker.setStatus(LocationPermissionStatus.FINE_GRANTED)
        startJourneySessionUseCase()
        endJourneySessionUseCase()

        val resetResult = resetJourneySessionUseCase()
        assertTrue(resetResult is Result.Success)
        assertEquals(SessionStatus.IDLE, sessionRepository.sessionStatus.value)
        assertNull(sessionRepository.activeSession.value)
    }
}
