package io.stereov.singularity.auth.geolocation.mapper

import com.maxmind.geoip2.model.CityResponse
import io.stereov.singularity.auth.geolocation.dto.GeolocationResponse
import org.springframework.stereotype.Component

@Component
class GeolocationMapper {

    fun createGeolocationResponse(cityResponse: CityResponse): GeolocationResponse {

        return GeolocationResponse(
            ipAddress = cityResponse.traits.ipAddress,
            city = cityResponse.city,
            country = cityResponse.country,
            continent = cityResponse.continent,
            location = cityResponse.location,
        )
    }
}