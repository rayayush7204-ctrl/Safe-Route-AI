package com.saferouteai.domain.audio

/**
 * Thread-safe, bounded in-memory sliding window for numeric [AcousticFeatures].
 *
 * Invariants:
 * - Fixed maximum capacity; drops oldest entries when capacity is exceeded.
 * - Stores ONLY derived numeric feature summaries; zero raw PCM samples.
 * - Entirely volatile; reset clears all stored history.
 */
class BoundedFeatureWindow(
    val maxCapacity: Int = 50
) {
    private val buffer = ArrayDeque<AcousticFeatures>(maxCapacity)

    val size: Int
        @Synchronized get() = buffer.size

    val isEmpty: Boolean
        @Synchronized get() = buffer.isEmpty()

    /**
     * Appends a new feature entry, dropping the oldest if at capacity.
     */
    @Synchronized
    fun add(features: AcousticFeatures) {
        if (buffer.size >= maxCapacity) {
            buffer.removeFirst()
        }
        buffer.addLast(features)
    }

    /**
     * Returns a snapshot copy of all entries in chronological order.
     */
    @Synchronized
    fun getAll(): List<AcousticFeatures> = buffer.toList()

    /**
     * Returns the most recent [count] entries in chronological order.
     */
    @Synchronized
    fun getRecent(count: Int): List<AcousticFeatures> {
        val n = count.coerceAtMost(buffer.size)
        if (n <= 0) return emptyList()
        return buffer.takeLast(n)
    }

    /**
     * Computes the baseline background RMS from the recent history prior to [skipLastN] frames.
     * Returns a safe noise floor (default 200.0) if insufficient history exists.
     */
    @Synchronized
    fun getBaselineRms(skipLastN: Int = 1, baselineWindow: Int = 10, floorRms: Double = 200.0): Double {
        val total = buffer.size
        val available = total - skipLastN
        if (available <= 0) return floorRms

        val start = (available - baselineWindow).coerceAtLeast(0)
        val baselineSlice = buffer.subList(start, available)
        if (baselineSlice.isEmpty()) return floorRms

        val avgRms = baselineSlice.map { it.rms }.average()
        return avgRms.coerceAtLeast(floorRms)
    }

    /**
     * Clears all historical entries.
     */
    @Synchronized
    fun clear() {
        buffer.clear()
    }
}
