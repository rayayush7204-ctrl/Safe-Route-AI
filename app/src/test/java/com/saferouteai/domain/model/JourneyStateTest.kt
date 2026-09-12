package com.saferouteai.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyStateTest {

    @Test
    fun idle_canOnlyTransitionToActive() {
        assertTrue(JourneyState.IDLE.canTransitionTo(JourneyState.ACTIVE))
        assertFalse(JourneyState.IDLE.canTransitionTo(JourneyState.COMPLETED))
        assertFalse(JourneyState.IDLE.canTransitionTo(JourneyState.IDLE))
    }

    @Test
    fun active_canOnlyTransitionToCompleted() {
        assertTrue(JourneyState.ACTIVE.canTransitionTo(JourneyState.COMPLETED))
        assertFalse(JourneyState.ACTIVE.canTransitionTo(JourneyState.IDLE))
        assertFalse(JourneyState.ACTIVE.canTransitionTo(JourneyState.ACTIVE))
    }

    @Test
    fun completed_canTransitionToIdleOrActive() {
        assertTrue(JourneyState.COMPLETED.canTransitionTo(JourneyState.IDLE))
        assertTrue(JourneyState.COMPLETED.canTransitionTo(JourneyState.ACTIVE))
        assertFalse(JourneyState.COMPLETED.canTransitionTo(JourneyState.COMPLETED))
    }
}
