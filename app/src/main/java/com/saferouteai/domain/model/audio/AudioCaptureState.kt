package com.saferouteai.domain.model.audio

/**
 * Explicit state machine for on-device foreground audio capture.
 *
 * Transitions:
 * IDLE -> STARTING -> CAPTURING -> STOPPING -> STOPPED -> IDLE
 * Any state -> ERROR -> IDLE
 */
enum class AudioCaptureState {
    IDLE,
    STARTING,
    CAPTURING,
    STOPPING,
    STOPPED,
    ERROR;

    val isCapturing: Boolean get() = this == CAPTURING
    val isActiveOrStarting: Boolean get() = this == STARTING || this == CAPTURING
}
