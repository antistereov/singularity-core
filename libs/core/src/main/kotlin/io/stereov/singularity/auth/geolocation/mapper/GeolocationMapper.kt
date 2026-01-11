package io.stereov.singularity.auth.geolocation.mapper

import com.maxmind.geoip2.model.CityResponse
import io.stereov.singularity.auth.geolocation.dto.GeolocationResponse
import org.springframework.stereotype.Component

/**
 * Responsible for mapping geolocation data from a CityResponse object to a GeolocationResponse object.
 */
@Component
class GeolocationMapper {

    /**
     * Creates a GeolocationResponse object based on the given CityResponse.
     *
     * @param cityResponse The CityResponse object containing geolocation data.
     * @return A GeolocationResponse object populated with data from the given CityResponse.
     */
    fun createGeolocationResponse(cityResponse: CityResponse): GeolocationResponse {

        return GeolocationResponse(
            ipAddress = cityResponse.traits().ipAddress().toString(),
            city = cityResponse.city(),
            country = cityResponse.country(),
            continent = cityResponse.continent(),
            location = cityResponse.location(),
        )
    }
}