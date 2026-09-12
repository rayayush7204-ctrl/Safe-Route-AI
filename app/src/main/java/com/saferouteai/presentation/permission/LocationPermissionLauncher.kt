package com.saferouteai.presentation.permission

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.saferouteai.domain.model.location.LocationPermissionStatus

@Composable
fun rememberLocationPermissionLauncher(
    onPermissionResult: (LocationPermissionStatus) -> Unit
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        val status = when {
            fineGranted -> LocationPermissionStatus.FINE_GRANTED
            coarseGranted -> LocationPermissionStatus.COARSE_GRANTED
            else -> LocationPermissionStatus.DENIED
        }
        onPermissionResult(status)
    }

    return {
        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
}
