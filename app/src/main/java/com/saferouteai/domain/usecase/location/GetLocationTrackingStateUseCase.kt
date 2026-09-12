package com.saferouteai.domain.usecase.location

import com.saferouteai.domain.model.location.LocationTrackingState
import com.saferouteai.domain.repository.LocationRepository
import kotlinx.coroutines.flow.StateFlow

class GetLocationTrackingStateUseCase(
    private val locationRepository: LocationRepository
) {
    operator fun invoke(): StateFlow<LocationTrackingState> = locationRepository.trackingState
}
