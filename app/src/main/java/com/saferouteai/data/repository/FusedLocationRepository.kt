package com.saferouteai.data.repository

import com.saferouteai.core.result.Result
import com.saferouteai.data.location.AndroidLocationPermissionChecker
import com.saferouteai.data.location.FusedLocationDataSource
import com.saferouteai.domain.model.location.LocationTrackingState
import com.saferouteai.domain.model.location.UserLocation
import com.saferouteai.domain.repository.LocationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FusedLocationRepository(
    private val locationDataSource: FusedLocationDataSource,
    private val permissionChecker: AndroidLocationPermissionChecker,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : LocationRepository {

    private val mutex = Mutex()
    private var trackingJob: Job? = null

    private val _trackingState = MutableStateFlow(LocationTrackingState.IDLE)
    override val trackingState: StateFlow<LocationTrackingState> = _trackingState.asStateFlow()

    private val _currentLocation = MutableStateFlow<UserLocation?>(null)
    override val currentLocation: StateFlow<UserLocation?> = _currentLocation.asStateFlow()

    private val _locationUpdates = MutableSharedFlow<UserLocation>(replay = 1)
    override val locationUpdates: Flow<UserLocation> = _locationUpdates.asSharedFlow()

    override suspend fun startTracking(): Result<Unit> = mutex.withLock {
        if (_trackingState.value == LocationTrackingState.TRACKING) {
            return Result.Success(Unit)
        }

        val isApproximate = !permissionChecker.permissionStatus.value.isPrecise

        trackingJob?.cancel()
        _trackingState.value = LocationTrackingState.TRACKING

        trackingJob = externalScope.launch {
            try {
                locationDataSource.getLocationUpdates(isApproximate).collect { location ->
                    _currentLocation.value = location
                    _locationUpdates.emit(location)
                }
            } catch (e: Exception) {
                _trackingState.value = LocationTrackingState.ERROR
            }
        }

        Result.Success(Unit)
    }

    override suspend fun pauseTracking(): Result<Unit> = mutex.withLock {
        if (_trackingState.value == LocationTrackingState.TRACKING) {
            trackingJob?.cancel()
            trackingJob = null
            _trackingState.value = LocationTrackingState.PAUSED
        }
        Result.Success(Unit)
    }

    override suspend fun resumeTracking(): Result<Unit> = mutex.withLock {
        if (_trackingState.value == LocationTrackingState.PAUSED) {
            val isApproximate = !permissionChecker.permissionStatus.value.isPrecise
            _trackingState.value = LocationTrackingState.TRACKING

            trackingJob?.cancel()
            trackingJob = externalScope.launch {
                try {
                    locationDataSource.getLocationUpdates(isApproximate).collect { location ->
                        _currentLocation.value = location
                        _locationUpdates.emit(location)
                    }
                } catch (e: Exception) {
                    _trackingState.value = LocationTrackingState.ERROR
                }
            }
        }
        Result.Success(Unit)
    }

    override suspend fun stopTracking(): Result<Unit> = mutex.withLock {
        trackingJob?.cancel()
        trackingJob = null
        _currentLocation.value = null
        _trackingState.value = LocationTrackingState.STOPPED
        Result.Success(Unit)
    }
}
