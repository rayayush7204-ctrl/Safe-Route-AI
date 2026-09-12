package com.saferouteai.domain.repository

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.location.LocationTrackingState
import com.saferouteai.domain.model.location.UserLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain contract for managing foreground safe journey location tracking.
 */
interface LocationRepository {
    /**
     * Observable stream of the current location tracking state machine.
     */
    val trackingState: StateFlow<LocationTrackingState>

    /**
     * Observable stream of the most recently received user location, or null if unacquired/stopped.
     */
    val currentLocation: StateFlow<UserLocation?>

    /**
     * Real-time stream of incoming location coordinate emissions.
     */
    val locationUpdates: Flow<UserLocation>

    /**
     * Starts foreground location tracking. Requires both consent and permission.
     */
    suspend fun startTracking(): Result<Unit>

    /**
     * Pauses location tracking when the application leaves the foreground.
     * No location coordinates are emitted while paused.
     */
    suspend fun pauseTracking(): Result<Unit>

    /**
     * Resumes location tracking when the application returns to the foreground.
     * Re-verifies that both consent and permission remain satisfied.
     */
    suspend fun resumeTracking(): Result<Unit>

    /**
     * Concludes location tracking and cleans up volatile in-memory coordinates.
     */
    suspend fun stopTracking(): Result<Unit>
}
