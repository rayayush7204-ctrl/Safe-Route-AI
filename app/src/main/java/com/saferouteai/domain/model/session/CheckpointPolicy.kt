package com.saferouteai.domain.model.session

/**
 * Domain configuration governing local periodic safety checkpoints.
 *
 * @property enabled Whether periodic check-ins are active for the session.
 * @property intervalMs Interval between checkpoints in milliseconds.
 * @property acknowledgementWindowMs Grace period to respond before checkpoint is marked missed.
 */
data class CheckpointPolicy(
    val enabled: Boolean = true,
    val intervalMs: Long = DEFAULT_INTERVAL_MS,
    val acknowledgementWindowMs: Long = DEFAULT_ACK_WINDOW_MS
) {
    init {
        require(intervalMs > 0) { "intervalMs must be positive: $intervalMs" }
        require(acknowledgementWindowMs > 0) { "acknowledgementWindowMs must be positive: $acknowledgementWindowMs" }
    }

    companion object {
        const val DEFAULT_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes
        const val DEFAULT_ACK_WINDOW_MS = 3 * 60 * 1000L // 3 minutes
    }
}
