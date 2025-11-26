package io.stereov.singularity.auth.geolocation.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onFailure
import com.maxmind.geoip2.model.CityResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.geolocation.dto.GeolocationResponse
import io.stereov.singularity.auth.geolocation.exception.GeolocationException
import io.stereov.singularity.auth.geolocation.mapper.GeolocationMapper
import io.stereov.singularity.auth.geolocation.properties.GeolocationProperties
import io.stereov.singularity.global.util.getClientIp
import io.stereov.singularity.global.util.getOrNull
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.net.InetAddress

/**
 * Service that provides functionalities for resolving geolocation details based on IP addresses.
 *
 * This service integrates with an underlying GeoIP database to retrieve geolocation
 * information such as city details. The service encapsulates business logic related to
 * geolocation and maps the retrieved data into desired response objects using a mapper.
 */
@Service
class GeolocationService(
    private val geolocationDatabaseService: GeolocationDatabaseService,
    private val properties: GeolocationProperties,
    private val geolocationMapper: GeolocationMapper,
) {

    private val logger = KotlinLogging.logger {}

    private suspend fun doGetLocation(ipAddress: InetAddress): Result<CityResponse, GeolocationException> {
        logger.debug { "Retrieving geolocation for IP address $ipAddress" }

        return geolocationDatabaseService.getCity(ipAddress)
    }

    /**
     * Retrieves the geolocation details for a given IP address.
     *
     * This method uses underlying services to fetch geolocation data and maps it
     * into a response object containing details such as city, country, continent, and location.
     *
     * @param ipAddress The IP address for which geolocation details need to be fetched.
     * @return A [Result] containing either a successful [GeolocationResponse] with geolocation details,
     *  or a [GeolocationException] in case of failure.
     */
    suspend fun getLocation(ipAddress: InetAddress): Result<GeolocationResponse, GeolocationException> {
        return doGetLocation(ipAddress)
            .map { cityResponse -> geolocationMapper.createGeolocationResponse(cityResponse) }
    }

    /**
     * Retrieves the geolocation details for a client based on their IP address extracted from the given server web exchange.
     *
     * This method attempts to resolve the client's IP address from the request, and if successful,
     * fetches the geolocation details. Returns null if the IP address cannot be determined or if the geolocation lookup fails.
     *
     * @param exchange The [ServerWebExchange] representing the client's incoming request.
     * @return A [GeolocationResponse] containing geolocation details if successful, or `null` if the IP address could not be resolved or geolocation lookup failed.
     */
    suspend fun getLocationOrNull(exchange: ServerWebExchange): GeolocationResponse? {
        val ipAddress = runCatching { exchange.request.getClientIp(properties.realIpHeader)?.let { InetAddress.getByName(it) } }

        return ipAddress.getOrNull()?.let {
            getLocation(it)
                .onFailure { ex -> logger.warn { "Failed to resolve geolocation for IP address $it: ${ex.message}"} }
                .getOrNull()
        }
    }
}
