package com.saferouteai.domain.location

import com.saferouteai.data.repository.InMemoryLocationRepository
import com.saferouteai.domain.model.location.LocationTrackingState
import com.saferouteai.domain.model.location.UserLocation
import com.saferouteai.domain.usecase.location.PauseLocationTrackingUseCase
import com.saferouteai.domain.usecase.location.StopLocationTrackingUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocationLifecycleAndStateTest {

    private lateinit var locationRepository: InMemoryLocationRepository
    private lateinit var pauseLocationTrackingUseCase: PauseLocationTrackingUseCase
    private lateinit var stopLocationTrackingUseCase: StopLocationTrackingUseCase

    @Before
    fun setUp() {
        locationRepository = InMemoryLocationRepository()
        pauseLocationTrackingUseCase = PauseLocationTrackingUseCase(locationRepository)
        stopLocationTrackingUseCase = StopLocationTrackingUseCase(locationRepository)
    }

    @Test
    fun initialState_isIdle() = runTest {
        assertEquals(LocationTrackingState.IDLE, locationRepository.trackingState.first())
        assertNull(locationRepository.currentLocation.first())
    }

    @Test
    fun trackingInForeground_emitsCoordinates() = runTest {
        locationRepository.startTracking()
        assertEquals(LocationTrackingState.TRACKING, locationRepository.trackingState.value)

        val testLocation = UserLocation(
            latitude = 37.4220,
            longitude = -122.0841,
            accuracyMeters = 5.0f,
            isApproximate = false
        )

        val emitted = locationRepository.emitLocation(testLocation)
        assertTrue(emitted)
        assertEquals(testLocation, locationRepository.currentLocation.value)
    }

    @Test
    fun transitionToPaused_whenLeavingForeground() = runTest {
        locationRepository.startTracking()
        assertEquals(LocationTrackingState.TRACKING, locationRepository.trackingState.value)

        val result = pauseLocationTrackingUseCase()
        assertTrue(result.isSuccess)
        assertEquals(LocationTrackingState.PAUSED, locationRepository.trackingState.value)
    }

    @Test
    fun noLocationEmissions_whilePaused() = runTest {
        locationRepository.startTracking()
        locationRepository.pauseTracking()
        assertEquals(LocationTrackingState.PAUSED, locationRepository.trackingState.value)

        val attemptedLocation = UserLocation(latitude = 10.0, longitude = 20.0)
        val emitted = locationRepository.emitLocation(attemptedLocation)

        assertFalse("Emissions must be dropped/blocked while tracking is PAUSED", emitted)
        assertNull("Current location must not be updated while PAUSED", locationRepository.currentLocation.value)
    }

    @Test
    fun resumeFromPaused_restoresTracking() = runTest {
        locationRepository.startTracking()
        locationRepository.pauseTracking()
        assertEquals(LocationTrackingState.PAUSED, locationRepository.trackingState.value)

        locationRepository.resumeTracking()
        assertEquals(LocationTrackingState.TRACKING, locationRepository.trackingState.value)

        val newLocation = UserLocation(latitude = 40.7128, longitude = -74.0060)
        assertTrue(locationRepository.emitLocation(newLocation))
        assertEquals(newLocation, locationRepository.currentLocation.value)
    }

    @Test
    fun endingJourney_transitionsToStopped_andClearsVolatileCoordinates() = runTest {
        locationRepository.startTracking()
        locationRepository.emitLocation(UserLocation(latitude = 51.5074, longitude = -0.1278))
        assertNotNull(locationRepository.currentLocation.value)

        val stopResult = stopLocationTrackingUseCase()
        assertTrue(stopResult.isSuccess)

        assertEquals(LocationTrackingState.STOPPED, locationRepository.trackingState.value)
        assertNull("Ending journey must wipe volatile in-memory coordinates", locationRepository.currentLocation.value)
    }

    @Test
    fun repeatedStartCalls_areIdempotent() = runTest {
        locationRepository.startTracking()
        assertEquals(LocationTrackingState.TRACKING, locationRepository.trackingState.value)

        val secondStart = locationRepository.startTracking()
        assertTrue(secondStart.isSuccess)
        assertEquals(LocationTrackingState.TRACKING, locationRepository.trackingState.value)
    }

    @Test
    fun repeatedStopCalls_areIdempotent() = runTest {
        locationRepository.startTracking()
        locationRepository.stopTracking()
        assertEquals(LocationTrackingState.STOPPED, locationRepository.trackingState.value)

        val secondStop = locationRepository.stopTracking()
        assertTrue(secondStop.isSuccess)
        assertEquals(LocationTrackingState.STOPPED, locationRepository.trackingState.value)
    }
}
