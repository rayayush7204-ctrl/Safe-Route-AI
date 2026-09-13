package com.saferouteai.domain.audio

import com.saferouteai.domain.model.audio.AcousticSignal
import com.saferouteai.domain.model.audio.AudioFrame

/**
 * Deterministic baseline detector for Milestone 5A.
 *
 * Processes PCM frames without classifying distress or wake words,
 * safely returning an empty signal list.
 */
class NoOpAcousticSignalDetector : AcousticSignalDetector {
    override fun processFrame(frame: AudioFrame): List<AcousticSignal> {
        // Intentionally no-op: raw PCM frames are received and immediately discarded
        return emptyList()
    }

    override fun reset() {
        // No state to clear
    }
}
