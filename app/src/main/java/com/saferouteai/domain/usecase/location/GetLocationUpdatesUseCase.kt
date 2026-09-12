package com.saferouteai.domain.usecase.location

import com.saferouteai.domain.model.location.UserLocation
import com.saferouteai.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow

class GetLocationUpdatesUseCase(
    private val locationRepository: LocationRepository
) {
    operator fun invoke(): Flow<UserLocation> = locationRepository.locationUpdates
}
