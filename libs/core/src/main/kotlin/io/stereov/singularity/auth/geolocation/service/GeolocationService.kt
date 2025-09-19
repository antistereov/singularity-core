package io.stereov.singularity.auth.geolocation.service

import com.maxmind.geoip2.model.CityResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.geolocation.dto.GeolocationResponse
import io.stereov.singularity.auth.geolocation.exception.GeolocationException
import io.stereov.singularity.auth.geolocation.mapper.GeolocationMapper
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
    private val geolocationMapper: GeolocationMapper,
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Retrieves the geolocation information for a given IP address.
     *
     * @param ipAddress The IP address to retrieve geolocation information for.
     * @return A [GeoLocationResponse] containing the geolocation information.
     * @throws GeolocationException If there is an error retrieving the geolocation information.
     */
    suspend fun getLocation(ipAddress: InetAddress): CityResponse {

        return try {
            geoIpDatabaseService.getCity(ipAddress)
        } catch (e: Exception) {
            throw GeolocationException("Unable to retrieve current geolocation for IP address $ipAddress", e)
        }
    }

    suspend fun getLocation(request: ServerHttpRequest): GeolocationResponse {
        val ipAddress = request.getClientIp(properties.realIpHeader)

        val cityResponse = getLocation(InetAddress.getByName(ipAddress))

        return geolocationMapper.createGeolocationResponse(cityResponse)
    }

    suspend fun getLocationOrNull(request: ServerHttpRequest): GeolocationResponse? {
        return runCatching { getLocation(request) }
            .getOrElse { error ->
                logger.warn { error.message }
                null
            }
    }
}
