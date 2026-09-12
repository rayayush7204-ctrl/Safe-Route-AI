package com.saferouteai.data.repository

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.location.LocationTrackingState
import com.saferouteai.domain.model.location.UserLocation
import com.saferouteai.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryLocationRepository(
    initialState: LocationTrackingState = LocationTrackingState.IDLE
) : LocationRepository {

    private val mutex = Mutex()

    private val _trackingState = MutableStateFlow(initialState)
    override val trackingState: StateFlow<LocationTrackingState> = _trackingState.asStateFlow()

    private val _currentLocation = MutableStateFlow<UserLocation?>(null)
    override val currentLocation: StateFlow<UserLocation?> = _currentLocation.asStateFlow()

    private val _locationUpdates = MutableSharedFlow<UserLocation>(replay = 1)
    override val locationUpdates: Flow<UserLocation> = _locationUpdates.asSharedFlow()

    fun setTrackingState(state: LocationTrackingState) {
        _trackingState.value = state
    }

    suspend fun emitLocation(location: UserLocation): Boolean {
        // Enforce: no emissions when PAUSED or not TRACKING
        if (_trackingState.value != LocationTrackingState.TRACKING) {
            return false
        }
        _currentLocation.value = location
        _locationUpdates.emit(location)
        return true
    }

    override suspend fun startTracking(): Result<Unit> = mutex.withLock {
        if (_trackingState.value == LocationTrackingState.TRACKING) {
            return Result.Success(Unit)
        }
        _trackingState.value = LocationTrackingState.TRACKING
        Result.Success(Unit)
    }

    override suspend fun pauseTracking(): Result<Unit> = mutex.withLock {
        if (_trackingState.value == LocationTrackingState.TRACKING) {
            _trackingState.value = LocationTrackingState.PAUSED
        }
        Result.Success(Unit)
    }

    override suspend fun resumeTracking(): Result<Unit> = mutex.withLock {
        if (_trackingState.value == LocationTrackingState.PAUSED) {
            _trackingState.value = LocationTrackingState.TRACKING
        }
        Result.Success(Unit)
    }

    override suspend fun stopTracking(): Result<Unit> = mutex.withLock {
        _currentLocation.value = null
        _trackingState.value = LocationTrackingState.STOPPED
        Result.Success(Unit)
    }
}
