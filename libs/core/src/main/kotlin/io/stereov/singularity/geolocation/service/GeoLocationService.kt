package io.stereov.singularity.geolocation.service

import io.stereov.singularity.geolocation.exception.GeoLocationException
import io.stereov.singularity.geolocation.model.GeoLocationResponse
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

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
    private val webClient: WebClient,
) {

    /**
     * Retrieves the geolocation information for a given IP address.
     *
     * @param ipAddress The IP address to retrieve geolocation information for.
     * @return A [GeoLocationResponse] containing the geolocation information.
     * @throws GeoLocationException If there is an error retrieving the geolocation information.
     */
    suspend fun getLocation(ipAddress: String): GeoLocationResponse {
        val uri = "https://freeipapi.com/api/json/$ipAddress"

        return try {
            webClient.get()
                .uri(uri)
                .retrieve()
                .awaitBody<GeoLocationResponse>()
        } catch (e: Exception) {
            throw GeoLocationException("Unable to retrieve current geolocation", e)
        }
    }
}
