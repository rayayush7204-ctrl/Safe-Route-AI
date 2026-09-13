package com.saferouteai.domain.model.anomaly

import java.util.UUID

/**
 * Immutable domain representation of an explainable, local anomaly observation.
 *
 * Invariant: ANOMALY != DANGER. AnomalySignal is a factual heuristic observation
 * that may later be consumed by risk fusion layers.
 *
 * @property id Unique identifier for this detection event.
 * @property sessionId The active journey session this observation belongs to.
 * @property type The classification of anomaly observed.
 * @property severity Observation significance tier (LOW, MEDIUM, HIGH).
 * @property timestampEpochMs Epoch timestamp when the observation was triggered.
 * @property explanation Human-readable factual summary of the observed conditions.
 * @property confidence Metric from 0.0 to 1.0 representing heuristic signal certainty.
 */
data class AnomalySignal(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val type: AnomalyType,
    val severity: AnomalySeverity,
    val timestampEpochMs: Long,
    val explanation: String,
    val confidence: Float = 1.0f
)
