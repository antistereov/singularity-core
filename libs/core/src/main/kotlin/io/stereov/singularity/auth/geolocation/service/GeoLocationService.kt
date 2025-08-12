package io.stereov.singularity.auth.geolocation.service

import com.maxmind.geoip2.model.CityResponse
import io.stereov.singularity.auth.geolocation.exception.GeoLocationException
import io.stereov.singularity.auth.geolocation.model.GeoLocationResponse
import org.springframework.stereotype.Service
import java.net.InetAddress

/**
 * # GeoLocationService
 *
 * This service is responsible for retrieving geolocation information based on an IP address.
 * It uses the `WebClient` to make HTTP requests to a geolocation API.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Service
class GeoLocationService(
    private val geoIpDatabaseService: GeoIpDatabaseService
) {

    /**
     * Retrieves the geolocation information for a given IP address.
     *
     * @param ipAddress The IP address to retrieve geolocation information for.
     * @return A [GeoLocationResponse] containing the geolocation information.
     * @throws GeoLocationException If there is an error retrieving the geolocation information.
     */
    suspend fun getLocation(ipAddress: InetAddress): CityResponse {

        return try {
            geoIpDatabaseService.getCity(ipAddress)
        } catch (e: Exception) {
            throw GeoLocationException("Unable to retrieve current geolocation", e)
        }
    }
}
