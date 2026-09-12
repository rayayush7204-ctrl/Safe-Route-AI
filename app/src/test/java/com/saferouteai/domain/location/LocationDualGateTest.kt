package com.saferouteai.domain.location

import com.saferouteai.data.repository.InMemoryConsentRepository
import com.saferouteai.data.repository.InMemoryLocationRepository
import com.saferouteai.domain.model.UserConsent
import com.saferouteai.domain.model.location.LocationPermissionStatus
import com.saferouteai.domain.model.location.LocationTrackingState
import com.saferouteai.domain.repository.FakeLocationPermissionChecker
import com.saferouteai.domain.usecase.location.ResumeLocationTrackingUseCase
import com.saferouteai.domain.usecase.location.StartLocationTrackingUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocationDualGateTest {

    private lateinit var locationRepository: InMemoryLocationRepository
    private lateinit var consentRepository: InMemoryConsentRepository
    private lateinit var permissionChecker: FakeLocationPermissionChecker
    private lateinit var startLocationTrackingUseCase: StartLocationTrackingUseCase
    private lateinit var resumeLocationTrackingUseCase: ResumeLocationTrackingUseCase

    @Before
    fun setUp() {
        locationRepository = InMemoryLocationRepository()
        consentRepository = InMemoryConsentRepository(initialConsent = UserConsent(locationSharingConsent = false))
        permissionChecker = FakeLocationPermissionChecker(initialStatus = LocationPermissionStatus.DENIED)
        startLocationTrackingUseCase = StartLocationTrackingUseCase(locationRepository, consentRepository, permissionChecker)
        resumeLocationTrackingUseCase = ResumeLocationTrackingUseCase(locationRepository, consentRepository, permissionChecker)
    }

    @Test
    fun neitherGateSatisfied_failsToStart() = runTest {
        val result = startLocationTrackingUseCase()
        assertTrue(result.isError)
        assertEquals(LocationTrackingState.IDLE, locationRepository.trackingState.value)
    }

    @Test
    fun onlyPermissionGranted_consentMissing_failsToStart() = runTest {
        permissionChecker.setStatus(LocationPermissionStatus.FINE_GRANTED)
        val result = startLocationTrackingUseCase()
        assertTrue(result.isError)
        assertTrue(result.exceptionOrNull()?.message?.contains("consent") == true)
        assertEquals(LocationTrackingState.IDLE, locationRepository.trackingState.value)
    }

    @Test
    fun onlyConsentGranted_permissionMissing_failsToStart() = runTest {
        consentRepository.setLocationSharingConsent(true)
        val result = startLocationTrackingUseCase()
        assertTrue(result.isError)
        assertTrue(result.exceptionOrNull()?.message?.contains("permission") == true)
        assertEquals(LocationTrackingState.IDLE, locationRepository.trackingState.value)
    }

    @Test
    fun bothGatesSatisfied_startsTrackingSuccessfully() = runTest {
        consentRepository.setLocationSharingConsent(true)
        permissionChecker.setStatus(LocationPermissionStatus.FINE_GRANTED)

        val result = startLocationTrackingUseCase()
        assertTrue(result.isSuccess)
        assertEquals(LocationTrackingState.TRACKING, locationRepository.trackingState.value)
    }

    @Test
    fun bothGatesSatisfiedWithCoarseLocation_startsTrackingSuccessfully() = runTest {
        consentRepository.setLocationSharingConsent(true)
        permissionChecker.setStatus(LocationPermissionStatus.COARSE_GRANTED)

        val result = startLocationTrackingUseCase()
        assertTrue(result.isSuccess)
        assertEquals(LocationTrackingState.TRACKING, locationRepository.trackingState.value)
    }

    @Test
    fun resumeTracking_failsIfConsentRevokedWhilePaused() = runTest {
        consentRepository.setLocationSharingConsent(true)
        permissionChecker.setStatus(LocationPermissionStatus.FINE_GRANTED)
        startLocationTrackingUseCase()
        locationRepository.pauseTracking()
        assertEquals(LocationTrackingState.PAUSED, locationRepository.trackingState.value)

        // Consent revoked during pause
        consentRepository.setLocationSharingConsent(false)

        val resumeResult = resumeLocationTrackingUseCase()
        assertTrue(resumeResult.isError)
        assertEquals(LocationTrackingState.STOPPED, locationRepository.trackingState.value)
    }

    @Test
    fun resumeTracking_failsIfPermissionRevokedWhilePaused() = runTest {
        consentRepository.setLocationSharingConsent(true)
        permissionChecker.setStatus(LocationPermissionStatus.FINE_GRANTED)
        startLocationTrackingUseCase()
        locationRepository.pauseTracking()
        assertEquals(LocationTrackingState.PAUSED, locationRepository.trackingState.value)

        // Permission revoked during pause
        permissionChecker.setStatus(LocationPermissionStatus.DENIED)

        val resumeResult = resumeLocationTrackingUseCase()
        assertTrue(resumeResult.isError)
        assertEquals(LocationTrackingState.STOPPED, locationRepository.trackingState.value)
    }
}
