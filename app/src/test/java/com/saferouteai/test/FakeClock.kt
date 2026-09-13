package com.saferouteai.test

import com.saferouteai.domain.time.Clock

/**
 * Deterministic, manual test double for [Clock].
 * Allows advancing time instantly without sleeping.
 */
class FakeClock(private var currentEpochMs: Long = 0L) : Clock {

    override fun nowEpochMs(): Long = currentEpochMs

    fun advanceTimeBy(millis: Long) {
        require(millis >= 0) { "Cannot advance time by negative duration: $millis" }
        currentEpochMs += millis
    }

    fun setTime(millis: Long) {
        currentEpochMs = millis
    }
}
