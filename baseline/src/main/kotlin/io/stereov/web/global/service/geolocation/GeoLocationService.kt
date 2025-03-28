package io.stereov.web.global.service.geolocation

import io.stereov.web.global.service.geolocation.exception.GeoLocationException
import io.stereov.web.global.service.geolocation.model.GeoLocationResponse
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Service
class GeoLocationService(
    private val webClient: WebClient,
) {

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
