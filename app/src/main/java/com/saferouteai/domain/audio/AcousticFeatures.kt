package com.saferouteai.domain.audio

/**
 * Derived acoustic feature metrics calculated from a single volatile PCM audio frame.
 *
 * Invariants:
 * - Contains ONLY numeric metrics.
 * - Raw PCM audio samples are strictly discarded and never retained.
 */
data class AcousticFeatures(
    val timestampEpochMs: Long,
    val sampleCount: Int,
    val sampleRate: Int,
    val rms: Double,
    val peakAmplitude: Short,
    val zeroCrossingRate: Double,
    val highFrequencyRatio: Double,
    val energy: Double,
    val durationMs: Long
)
