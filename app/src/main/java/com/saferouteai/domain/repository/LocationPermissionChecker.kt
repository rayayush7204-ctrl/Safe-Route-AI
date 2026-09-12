package com.saferouteai.domain.repository

import com.saferouteai.domain.model.location.LocationPermissionStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Pure Kotlin abstraction to query and observe runtime location permission status.
 */
interface LocationPermissionChecker {
    val permissionStatus: StateFlow<LocationPermissionStatus>

    fun hasLocationPermission(): Boolean
}
