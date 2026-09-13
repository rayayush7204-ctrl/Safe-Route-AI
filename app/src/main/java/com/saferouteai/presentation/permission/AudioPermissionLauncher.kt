package com.saferouteai.presentation.permission

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.saferouteai.domain.model.audio.AudioPermissionStatus

@Composable
fun rememberAudioPermissionLauncher(
    onPermissionResult: (AudioPermissionStatus) -> Unit
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val status = if (isGranted) AudioPermissionStatus.GRANTED else AudioPermissionStatus.DENIED
        onPermissionResult(status)
    }

    return {
        launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
}
