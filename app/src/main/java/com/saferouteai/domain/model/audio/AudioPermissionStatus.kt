package com.saferouteai.domain.model.audio

/**
 * Pure Kotlin representation of Android RECORD_AUDIO runtime permission status.
 */
enum class AudioPermissionStatus {
    NOT_REQUESTED,
    GRANTED,
    DENIED;

    val isGranted: Boolean get() = this == GRANTED
}
