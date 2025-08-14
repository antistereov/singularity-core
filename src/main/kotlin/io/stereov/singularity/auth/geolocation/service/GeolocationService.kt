package io.stereov.singularity.auth.geolocation.service

import com.maxmind.geoip2.model.CityResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.geolocation.exception.GeoLocationException
import io.stereov.singularity.auth.geolocation.model.GeoLocationResponse
import io.stereov.singularity.auth.geolocation.properties.GeolocationProperties
import io.stereov.singularity.global.util.getClientIp
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Service
import java.net.InetAddress

/**
 * # GeolocationService
 *
 * This service is responsible for retrieving geolocation information based on an IP address.
 * It uses the `WebClient` to make HTTP requests to a geolocation API.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Service
class GeolocationService(
    private val geoIpDatabaseService: GeoIpDatabaseService,
    private val properties: GeolocationProperties,
) {

    private val logger = KotlinLogging.logger {}

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
            throw GeoLocationException("Unable to retrieve current geolocation for IP address $ipAddress", e)
        }
    }

    suspend fun getLocation(request: ServerHttpRequest): CityResponse {
        val ipAddress = request.getClientIp(properties.realIpHeader)

        return getLocation(InetAddress.getByName(ipAddress))
    }

    suspend fun getLocationOrNull(request: ServerHttpRequest): CityResponse? {
        return runCatching { getLocation(request) }
            .onFailure { error ->
                logger.warn { error.message }
                null
            }
            .getOrNull()
    }
}
