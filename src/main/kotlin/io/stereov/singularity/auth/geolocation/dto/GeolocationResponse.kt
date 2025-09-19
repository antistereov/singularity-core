package io.stereov.singularity.auth.geolocation.dto

import com.maxmind.geoip2.record.City
import com.maxmind.geoip2.record.Continent
import com.maxmind.geoip2.record.Country
import com.maxmind.geoip2.record.Location

data class GeolocationResponse(
    val ipAddress: String,
    val city: City,
    val country: Country,
    val continent: Continent,
    val location: Location
)
