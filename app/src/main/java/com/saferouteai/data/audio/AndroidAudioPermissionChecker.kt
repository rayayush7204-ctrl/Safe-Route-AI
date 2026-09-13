package com.saferouteai.data.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.saferouteai.domain.model.audio.AudioPermissionStatus
import com.saferouteai.domain.repository.AudioPermissionChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidAudioPermissionChecker(
    private val context: Context
) : AudioPermissionChecker {

    private val _permissionStatus = MutableStateFlow(determineCurrentStatus())
    override val permissionStatus: StateFlow<AudioPermissionStatus> = _permissionStatus.asStateFlow()

    fun updateStatus(status: AudioPermissionStatus) {
        _permissionStatus.value = status
    }

    fun refreshStatus(): AudioPermissionStatus {
        val current = determineCurrentStatus()
        _permissionStatus.value = current
        return current
    }

    override fun hasAudioPermission(): Boolean {
        return refreshStatus().isGranted
    }

    private fun determineCurrentStatus(): AudioPermissionStatus {
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        return if (isGranted) AudioPermissionStatus.GRANTED else AudioPermissionStatus.DENIED
    }
}
