package com.saferouteai.domain.model.audio

/**
 * Sealed hierarchy of typed audio capture errors.
 */
sealed class AudioError(open val message: String) {
    data object ConsentRequired : AudioError("Explicit audio processing consent is required")
    data object PermissionRequired : AudioError("Android RECORD_AUDIO runtime permission is required")
    data object InactiveSession : AudioError("Audio capture requires an active Safe Journey session")
    data object DeviceUnavailable : AudioError("Audio recording hardware is unavailable or in use")
    data object AlreadyCapturing : AudioError("Audio capture is already running")
    data class InitializationFailed(val reason: String) : AudioError("AudioRecord initialization failed: $reason")
    data class RecordingFailed(val reason: String) : AudioError("Audio recording loop failed: $reason")
}

class AudioCaptureException(val error: AudioError) : Exception(error.message)
