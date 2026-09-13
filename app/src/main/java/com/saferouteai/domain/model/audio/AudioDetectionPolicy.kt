package com.saferouteai.domain.model.audio

/**
 * Configuration parameters for secure on-device audio capture.
 *
 * Invariants:
 * - Strict 16 kHz Mono 16-bit PCM configuration.
 * - 100ms frame chunks (1600 samples) in volatile memory only.
 * - Max 30 retained signals in RAM sliding window.
 */
data class AudioDetectionPolicy(
    val sampleRate: Int = 16000,
    val channelConfig: Int = 1, // Mono
    val audioEncodingBits: Int = 16, // 16-bit PCM
    val frameSizeMs: Int = 100, // 100ms chunks -> 1600 samples
    val maxRetainedSignals: Int = 30
) {
    val frameSizeInSamples: Int
        get() = (sampleRate * frameSizeMs) / 1000

    val frameSizeInBytes: Int
        get() = frameSizeInSamples * (audioEncodingBits / 8) * channelConfig
}
