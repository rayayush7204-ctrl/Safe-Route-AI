package com.saferouteai.domain.repository

import com.saferouteai.domain.model.audio.AudioPermissionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAudioPermissionChecker(
    initialStatus: AudioPermissionStatus = AudioPermissionStatus.NOT_REQUESTED
) : AudioPermissionChecker {

    private val _permissionStatus = MutableStateFlow(initialStatus)
    override val permissionStatus: StateFlow<AudioPermissionStatus> = _permissionStatus.asStateFlow()

    fun setStatus(status: AudioPermissionStatus) {
        _permissionStatus.value = status
    }

    override fun hasAudioPermission(): Boolean = _permissionStatus.value.isGranted
}
