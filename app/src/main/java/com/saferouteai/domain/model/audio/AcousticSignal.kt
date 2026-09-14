package com.saferouteai.domain.model.audio

import java.util.UUID

/**
 * Pure domain representation of a detected acoustic observation.
 *
 * Invariants:
 * - Represents an on-device observational signal only.
 * - Does NOT indicate confirmed emergency or trigger external dispatch.
 */
data class AcousticSignal(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String = "",
    val type: AcousticSignalType,
    val detectedAtEpochMs: Long,
    val durationMs: Long? = null,
    val explanation: String,
    val confidence: Float? = null,
    val metadata: Map<String, String> = emptyMap()
)
