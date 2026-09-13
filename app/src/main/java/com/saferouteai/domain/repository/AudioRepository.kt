package com.saferouteai.domain.repository

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.audio.AcousticSignal
import com.saferouteai.domain.model.audio.AudioCaptureState
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain repository contract for managing the on-device audio capture pipeline
 * and observing acoustic signals.
 */
interface AudioRepository {
    /**
     * Observable stream of current audio capture lifecycle state.
     */
    val captureState: StateFlow<AudioCaptureState>

    /**
     * Observable stream of detected acoustic signals in volatile memory.
     */
    val acousticSignals: StateFlow<List<AcousticSignal>>

    /**
     * Initiates foreground audio capture if all gates (session, consent, permission) are satisfied.
     */
    suspend fun startCapture(): Result<Unit>

    /**
     * Stops audio capture and safely releases platform resources.
     */
    suspend fun stopCapture(): Result<Unit>

    /**
     * Clears all in-memory acoustic signals upon session completion or reset.
     */
    suspend fun clearSignals(): Result<Unit>
}
