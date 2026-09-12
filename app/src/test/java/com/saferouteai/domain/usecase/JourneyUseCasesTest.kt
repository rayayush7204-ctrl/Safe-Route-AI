package com.saferouteai.domain.usecase

import com.saferouteai.data.repository.InMemoryJourneyRepository
import com.saferouteai.domain.model.JourneyState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JourneyUseCasesTest {

    private lateinit var repository: InMemoryJourneyRepository
    private lateinit var getJourneyStateUseCase: GetJourneyStateUseCase
    private lateinit var startJourneyUseCase: StartJourneyUseCase
    private lateinit var endJourneyUseCase: EndJourneyUseCase
    private lateinit var resetJourneyUseCase: ResetJourneyUseCase

    @Before
    fun setUp() {
        repository = InMemoryJourneyRepository()
        getJourneyStateUseCase = GetJourneyStateUseCase(repository)
        startJourneyUseCase = StartJourneyUseCase(repository)
        endJourneyUseCase = EndJourneyUseCase(repository)
        resetJourneyUseCase = ResetJourneyUseCase(repository)
    }

    @Test
    fun initialState_isIdle() {
        assertEquals(JourneyState.IDLE, getJourneyStateUseCase().value)
    }

    @Test
    fun startJourney_transitionsToActive() = runTest {
        val result = startJourneyUseCase()
        assertTrue(result.isSuccess)
        val journey = result.getOrNull()
        assertNotNull(journey)
        assertEquals(JourneyState.ACTIVE, journey?.state)
        assertEquals(JourneyState.ACTIVE, getJourneyStateUseCase().value)
    }

    @Test
    fun endJourney_transitionsToCompleted() = runTest {
        startJourneyUseCase()
        val result = endJourneyUseCase()
        assertTrue(result.isSuccess)
        val journey = result.getOrNull()
        assertNotNull(journey)
        assertEquals(JourneyState.COMPLETED, journey?.state)
        assertNotNull(journey?.endTimestampEpochMs)
        assertEquals(JourneyState.COMPLETED, getJourneyStateUseCase().value)
    }

    @Test
    fun endJourney_fromIdleState_failsWithIllegalStateException() = runTest {
        val result = endJourneyUseCase()
        assertTrue(result.isError)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals(JourneyState.IDLE, getJourneyStateUseCase().value)
    }

    @Test
    fun fullJourneyLifecycle_idleToActiveToCompletedToIdle() = runTest {
        // IDLE
        assertEquals(JourneyState.IDLE, getJourneyStateUseCase().value)

        // START -> ACTIVE
        assertTrue(startJourneyUseCase().isSuccess)
        assertEquals(JourneyState.ACTIVE, getJourneyStateUseCase().value)

        // END -> COMPLETED
        assertTrue(endJourneyUseCase().isSuccess)
        assertEquals(JourneyState.COMPLETED, getJourneyStateUseCase().value)

        // RESET -> IDLE
        assertTrue(resetJourneyUseCase().isSuccess)
        assertEquals(JourneyState.IDLE, getJourneyStateUseCase().value)
    }
}
