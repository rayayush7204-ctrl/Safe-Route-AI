package com.saferouteai.domain.model

/**
 * Domain model representing a personal safety journey.
 *
 * @property id Unique identifier for the journey session.
 * @property state Current state of the journey.
 * @property startTimestampEpochMs Timestamp when the journey was initiated (epoch ms).
 * @property endTimestampEpochMs Optional timestamp when the journey was concluded.
 */
data class Journey(
    val id: String,
    val state: JourneyState,
    val startTimestampEpochMs: Long,
    val endTimestampEpochMs: Long? = null
)
