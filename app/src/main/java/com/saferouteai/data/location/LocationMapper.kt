package com.saferouteai.data.location

import android.location.Location
import com.saferouteai.domain.model.location.UserLocation

object LocationMapper {
    fun toDomain(location: Location, isApproximate: Boolean = false): UserLocation {
        return UserLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            altitudeMeters = if (location.hasAltitude()) location.altitude else null,
            accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
            speedMps = if (location.hasSpeed()) location.speed else null,
            bearingDegrees = if (location.hasBearing()) location.bearing else null,
            timestampEpochMs = location.time,
            isApproximate = isApproximate
        )
    }
}
