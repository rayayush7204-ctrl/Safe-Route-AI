package com.saferouteai.domain.time

/**
 * Injectable time abstraction for deterministic temporal calculations and testing.
 */
interface Clock {
    /**
     * Returns the current time in milliseconds since the Unix epoch.
     */
    fun nowEpochMs(): Long
}
