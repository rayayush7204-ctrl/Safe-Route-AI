package com.saferouteai.domain.model.session

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionStatusTest {

    @Test
    fun `idle can only transition to starting`() {
        assertTrue(SessionStatus.IDLE.canTransitionTo(SessionStatus.STARTING))
        assertFalse(SessionStatus.IDLE.canTransitionTo(SessionStatus.ACTIVE))
        assertFalse(SessionStatus.IDLE.canTransitionTo(SessionStatus.COMPLETED))
        assertFalse(SessionStatus.IDLE.canTransitionTo(SessionStatus.CANCELLED))
        assertFalse(SessionStatus.IDLE.canTransitionTo(SessionStatus.CHECKPOINT_DUE))
    }

    @Test
    fun `starting can transition to active or cancelled`() {
        assertTrue(SessionStatus.STARTING.canTransitionTo(SessionStatus.ACTIVE))
        assertTrue(SessionStatus.STARTING.canTransitionTo(SessionStatus.CANCELLED))
        assertFalse(SessionStatus.STARTING.canTransitionTo(SessionStatus.IDLE))
        assertFalse(SessionStatus.STARTING.canTransitionTo(SessionStatus.COMPLETED))
        assertFalse(SessionStatus.STARTING.canTransitionTo(SessionStatus.CHECKPOINT_DUE))
    }

    @Test
    fun `active can transition to checkpoint due, completing, or cancelled`() {
        assertTrue(SessionStatus.ACTIVE.canTransitionTo(SessionStatus.CHECKPOINT_DUE))
        assertTrue(SessionStatus.ACTIVE.canTransitionTo(SessionStatus.COMPLETING))
        assertTrue(SessionStatus.ACTIVE.canTransitionTo(SessionStatus.CANCELLED))
        assertFalse(SessionStatus.ACTIVE.canTransitionTo(SessionStatus.IDLE))
        assertFalse(SessionStatus.ACTIVE.canTransitionTo(SessionStatus.STARTING))
        assertFalse(SessionStatus.ACTIVE.canTransitionTo(SessionStatus.COMPLETED))
    }

    @Test
    fun `checkpoint due can transition to acknowledged, completing, or cancelled`() {
        assertTrue(SessionStatus.CHECKPOINT_DUE.canTransitionTo(SessionStatus.CHECKPOINT_ACKNOWLEDGED))
        assertTrue(SessionStatus.CHECKPOINT_DUE.canTransitionTo(SessionStatus.COMPLETING))
        assertTrue(SessionStatus.CHECKPOINT_DUE.canTransitionTo(SessionStatus.CANCELLED))
        assertFalse(SessionStatus.CHECKPOINT_DUE.canTransitionTo(SessionStatus.ACTIVE))
        assertFalse(SessionStatus.CHECKPOINT_DUE.canTransitionTo(SessionStatus.IDLE))
    }

    @Test
    fun `checkpoint acknowledged can transition to active, completing, or cancelled`() {
        assertTrue(SessionStatus.CHECKPOINT_ACKNOWLEDGED.canTransitionTo(SessionStatus.ACTIVE))
        assertTrue(SessionStatus.CHECKPOINT_ACKNOWLEDGED.canTransitionTo(SessionStatus.COMPLETING))
        assertTrue(SessionStatus.CHECKPOINT_ACKNOWLEDGED.canTransitionTo(SessionStatus.CANCELLED))
        assertFalse(SessionStatus.CHECKPOINT_ACKNOWLEDGED.canTransitionTo(SessionStatus.CHECKPOINT_DUE))
    }

    @Test
    fun `completing can only transition to completed`() {
        assertTrue(SessionStatus.COMPLETING.canTransitionTo(SessionStatus.COMPLETED))
        assertFalse(SessionStatus.COMPLETING.canTransitionTo(SessionStatus.ACTIVE))
        assertFalse(SessionStatus.COMPLETING.canTransitionTo(SessionStatus.IDLE))
        assertFalse(SessionStatus.COMPLETING.canTransitionTo(SessionStatus.CANCELLED))
    }

    @Test
    fun `completed can only transition to idle`() {
        assertTrue(SessionStatus.COMPLETED.canTransitionTo(SessionStatus.IDLE))
        assertFalse(SessionStatus.COMPLETED.canTransitionTo(SessionStatus.ACTIVE))
        assertFalse(SessionStatus.COMPLETED.canTransitionTo(SessionStatus.STARTING))
        assertFalse(SessionStatus.COMPLETED.canTransitionTo(SessionStatus.CANCELLED))
    }

    @Test
    fun `cancelled can only transition to idle`() {
        assertTrue(SessionStatus.CANCELLED.canTransitionTo(SessionStatus.IDLE))
        assertFalse(SessionStatus.CANCELLED.canTransitionTo(SessionStatus.ACTIVE))
        assertFalse(SessionStatus.CANCELLED.canTransitionTo(SessionStatus.STARTING))
        assertFalse(SessionStatus.CANCELLED.canTransitionTo(SessionStatus.COMPLETED))
    }

    @Test
    fun `isActiveSession returns true only for in-progress states`() {
        assertFalse(SessionStatus.IDLE.isActiveSession)
        assertTrue(SessionStatus.STARTING.isActiveSession)
        assertTrue(SessionStatus.ACTIVE.isActiveSession)
        assertTrue(SessionStatus.CHECKPOINT_DUE.isActiveSession)
        assertTrue(SessionStatus.CHECKPOINT_ACKNOWLEDGED.isActiveSession)
        assertTrue(SessionStatus.COMPLETING.isActiveSession)
        assertFalse(SessionStatus.COMPLETED.isActiveSession)
        assertFalse(SessionStatus.CANCELLED.isActiveSession)
    }
}
