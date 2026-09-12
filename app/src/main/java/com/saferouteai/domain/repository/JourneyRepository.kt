package com.saferouteai.domain.repository

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.Journey
import com.saferouteai.domain.model.JourneyState
import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for managing the lifecycle and domain state of safe journeys.
 */
interface JourneyRepository {
    /**
     * Observable stream of the current journey state.
     */
    val journeyState: StateFlow<JourneyState>

    /**
     * Observable stream of the active or latest journey instance, or null if uninitialized.
     */
    val currentJourney: StateFlow<Journey?>

    /**
     * Initiates a new safe journey session.
     *
     * @return Result containing the newly created [Journey] or an error if invalid state.
     */
    suspend fun startJourney(): Result<Journey>

    /**
     * Concludes the currently active safe journey session.
     *
     * @return Result containing the concluded [Journey] or an error if no active journey.
     */
    suspend fun endJourney(): Result<Journey>

    /**
     * Resets the journey state back to [JourneyState.IDLE].
     */
    suspend fun resetToIdle(): Result<Unit>
}
