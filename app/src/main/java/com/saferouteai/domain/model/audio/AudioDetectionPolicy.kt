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
    val maxRetainedSignals: Int = 30,
    // Milestone 5B: Deterministic Acoustic Detection Thresholds
    val impactPeakThreshold: Short = 18000,
    val impactRmsThreshold: Double = 5000.0,
    val impactEnergyDeltaRatio: Double = 5.0,
    val screamMinSustainedFrames: Int = 3,
    val screamRmsThreshold: Double = 4500.0,
    val screamPeakThreshold: Short = 12000,
    val screamZcrMin: Double = 0.08,
    val screamZcrMax: Double = 0.55,
    val commotionWindowFrames: Int = 30,
    val commotionMinElevatedFrames: Int = 8,
    val commotionFrameRmsThreshold: Double = 3500.0,
    val commotionCooldownFrames: Int = 30,
    val maxFeatureHistoryFrames: Int = 50
) {
    val frameSizeInSamples: Int
        get() = (sampleRate * frameSizeMs) / 1000

    val frameSizeInBytes: Int
        get() = frameSizeInSamples * (audioEncodingBits / 8) * channelConfig
}
