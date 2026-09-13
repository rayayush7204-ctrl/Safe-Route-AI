package com.saferouteai.domain.repository

import com.saferouteai.domain.model.anomaly.AnomalySignal
import com.saferouteai.domain.model.location.UserLocation
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain repository contract for managing in-memory anomaly observations
 * and volatile recent location windows during an active journey.
 *
 * Invariant: Transient RAM storage only; zero Room persistence or disk storage.
 */
interface AnomalyRepository {

    /**
     * Observable stream of current active anomaly signals.
     */
    val activeSignals: StateFlow<List<AnomalySignal>>

    /**
     * Observable stream of recent in-memory location observations within the evaluation window.
     */
    val recentLocations: StateFlow<List<UserLocation>>

    /**
     * Appends a new volatile location observation to the sliding window.
     */
    fun addLocationObservation(location: UserLocation)

    /**
     * Updates the set of currently active detected anomaly signals.
     */
    fun updateSignals(signals: List<AnomalySignal>)

    /**
     * Clears all in-memory location observations and anomaly signals upon session conclusion or cancellation.
     */
    fun clear()
}
