package com.saferouteai.data.repository

import com.saferouteai.domain.model.anomaly.AnomalySignal
import com.saferouteai.domain.model.location.UserLocation
import com.saferouteai.domain.repository.AnomalyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections

/**
 * Pure in-memory thread-safe implementation of [AnomalyRepository].
 *
 * Maintains a bounded FIFO sliding window of recent volatile location coordinates
 * and active [AnomalySignal] observations without touching disk or SQLite.
 */
class InMemoryAnomalyRepository(
    private val maxLocationWindowSize: Int = DEFAULT_MAX_WINDOW_SIZE
) : AnomalyRepository {

    private val _activeSignals = MutableStateFlow<List<AnomalySignal>>(emptyList())
    override val activeSignals: StateFlow<List<AnomalySignal>> = _activeSignals.asStateFlow()

    private val _recentLocations = MutableStateFlow<List<UserLocation>>(emptyList())
    override val recentLocations: StateFlow<List<UserLocation>> = _recentLocations.asStateFlow()

    private val locationBuffer = Collections.synchronizedList(mutableListOf<UserLocation>())

    @Synchronized
    override fun addLocationObservation(location: UserLocation) {
        locationBuffer.add(location)
        while (locationBuffer.size > maxLocationWindowSize) {
            locationBuffer.removeAt(0)
        }
        _recentLocations.value = locationBuffer.toList()
    }

    @Synchronized
    override fun updateSignals(signals: List<AnomalySignal>) {
        _activeSignals.value = signals
    }

    @Synchronized
    override fun clear() {
        locationBuffer.clear()
        _recentLocations.value = emptyList()
        _activeSignals.value = emptyList()
    }

    companion object {
        const val DEFAULT_MAX_WINDOW_SIZE = 30
    }
}
