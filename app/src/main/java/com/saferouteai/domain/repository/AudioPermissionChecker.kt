package com.saferouteai.domain.repository

import com.saferouteai.domain.model.audio.AudioPermissionStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Pure Kotlin abstraction to query and observe runtime microphone permission status.
 */
interface AudioPermissionChecker {
    val permissionStatus: StateFlow<AudioPermissionStatus>

    fun hasAudioPermission(): Boolean
}
