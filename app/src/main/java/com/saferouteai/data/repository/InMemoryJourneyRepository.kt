package com.saferouteai.data.repository

import com.saferouteai.core.result.Result
import com.saferouteai.domain.model.Journey
import com.saferouteai.domain.model.JourneyState
import com.saferouteai.domain.repository.JourneyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * In-memory, thread-safe implementation of [JourneyRepository].
 *
 * Provides pure local state management for Milestone 1 without network or sensor side-effects.
 */
class InMemoryJourneyRepository : JourneyRepository {

    private val mutex = Mutex()

    private val _journeyState = MutableStateFlow(JourneyState.IDLE)
    override val journeyState: StateFlow<JourneyState> = _journeyState.asStateFlow()

    private val _currentJourney = MutableStateFlow<Journey?>(null)
    override val currentJourney: StateFlow<Journey?> = _currentJourney.asStateFlow()

    override suspend fun startJourney(): Result<Journey> = mutex.withLock {
        val currentState = _journeyState.value
        if (!currentState.canTransitionTo(JourneyState.ACTIVE)) {
            return Result.Error(
                IllegalStateException("Cannot start journey from state: $currentState")
            )
        }

        val newJourney = Journey(
            id = UUID.randomUUID().toString(),
            state = JourneyState.ACTIVE,
            startTimestampEpochMs = System.currentTimeMillis()
        )

        _currentJourney.value = newJourney
        _journeyState.value = JourneyState.ACTIVE

        Result.Success(newJourney)
    }

    override suspend fun endJourney(): Result<Journey> = mutex.withLock {
        val currentState = _journeyState.value
        if (!currentState.canTransitionTo(JourneyState.COMPLETED)) {
            return Result.Error(
                IllegalStateException("Cannot end journey from state: $currentState")
            )
        }

        val activeJourney = _currentJourney.value
        val completedJourney = activeJourney?.copy(
            state = JourneyState.COMPLETED,
            endTimestampEpochMs = System.currentTimeMillis()
        ) ?: Journey(
            id = UUID.randomUUID().toString(),
            state = JourneyState.COMPLETED,
            startTimestampEpochMs = System.currentTimeMillis(),
            endTimestampEpochMs = System.currentTimeMillis()
        )

        _currentJourney.value = completedJourney
        _journeyState.value = JourneyState.COMPLETED

        Result.Success(completedJourney)
    }

    override suspend fun resetToIdle(): Result<Unit> = mutex.withLock {
        _journeyState.value = JourneyState.IDLE
        _currentJourney.value = null
        Result.Success(Unit)
    }
}
