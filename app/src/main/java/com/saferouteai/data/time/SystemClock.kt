package com.saferouteai.data.time

import com.saferouteai.domain.time.Clock

/**
 * Production implementation of [Clock] delegating to the system clock.
 */
class SystemClock : Clock {
    override fun nowEpochMs(): Long = System.currentTimeMillis()
}
