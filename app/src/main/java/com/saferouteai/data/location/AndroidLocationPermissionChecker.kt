package com.saferouteai.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.saferouteai.domain.model.location.LocationPermissionStatus
import com.saferouteai.domain.repository.LocationPermissionChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidLocationPermissionChecker(
    private val context: Context
) : LocationPermissionChecker {

    private val _permissionStatus = MutableStateFlow(determineCurrentStatus())
    override val permissionStatus: StateFlow<LocationPermissionStatus> = _permissionStatus.asStateFlow()

    fun updateStatus(status: LocationPermissionStatus) {
        _permissionStatus.value = status
    }

    fun refreshStatus(): LocationPermissionStatus {
        val current = determineCurrentStatus()
        _permissionStatus.value = current
        return current
    }

    override fun hasLocationPermission(): Boolean {
        return refreshStatus().isGranted
    }

    private fun determineCurrentStatus(): LocationPermissionStatus {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return when {
            fineGranted -> LocationPermissionStatus.FINE_GRANTED
            coarseGranted -> LocationPermissionStatus.COARSE_GRANTED
            else -> LocationPermissionStatus.DENIED
        }
    }
}
