package com.saferouteai.domain.repository

import com.saferouteai.domain.model.location.LocationPermissionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeLocationPermissionChecker(
    initialStatus: LocationPermissionStatus = LocationPermissionStatus.NOT_REQUESTED
) : LocationPermissionChecker {

    private val _permissionStatus = MutableStateFlow(initialStatus)
    override val permissionStatus: StateFlow<LocationPermissionStatus> = _permissionStatus.asStateFlow()

    fun setStatus(status: LocationPermissionStatus) {
        _permissionStatus.value = status
    }

    override fun hasLocationPermission(): Boolean = _permissionStatus.value.isGranted
}
