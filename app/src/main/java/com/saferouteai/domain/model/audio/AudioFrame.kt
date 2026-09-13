package com.saferouteai.domain.model.audio

/**
 * Volatile in-memory representation of a single PCM audio frame.
 *
 * Invariants:
 * - Exists strictly in RAM during processing.
 * - Never persisted to disk, Room, or logged.
 */
class AudioFrame(
    val timestampEpochMs: Long,
    val pcmData: ShortArray,
    val sampleRate: Int = 16000
) {
    val sampleCount: Int get() = pcmData.size

    val durationMs: Long get() = (sampleCount * 1000L) / sampleRate

    override fun toString(): String {
        return "AudioFrame(timestampEpochMs=$timestampEpochMs, sampleCount=$sampleCount, sampleRate=$sampleRate)"
    }
}
