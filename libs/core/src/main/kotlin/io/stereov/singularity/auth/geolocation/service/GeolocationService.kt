package io.stereov.singularity.auth.geolocation.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.maxmind.geoip2.model.CityResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.geolocation.dto.GeolocationResponse
import io.stereov.singularity.auth.geolocation.exception.GeolocationException
import io.stereov.singularity.auth.geolocation.mapper.GeolocationMapper
import io.stereov.singularity.auth.geolocation.properties.GeolocationProperties
import org.springframework.stereotype.Service
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
}
