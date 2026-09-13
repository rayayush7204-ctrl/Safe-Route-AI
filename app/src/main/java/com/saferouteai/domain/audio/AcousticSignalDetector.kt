package com.saferouteai.domain.audio

import com.saferouteai.domain.model.audio.AcousticSignal
import com.saferouteai.domain.model.audio.AudioFrame

/**
 * Pluggable domain boundary for acoustic signal detection.
 * Future on-device ML distress/wake-word classifiers will implement this interface.
 */
interface AcousticSignalDetector {
    /**
     * Analyzes an in-memory PCM audio frame and returns any detected observational signals.
     */
    fun processFrame(frame: AudioFrame): List<AcousticSignal>

    /**
     * Resets any internal state when capture stops.
     */
    fun reset()
}
