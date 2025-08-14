package io.stereov.singularity.global.filter

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.geolocation.properties.GeolocationProperties
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.global.util.getClientIp
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * # Filter for logging incoming requests and outgoing responses.
 *
 * This ratelimit logs the details of incoming requests and outgoing responses,
 * including the HTTP method, path, and status code.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
class LoggingFilter(
    private val geolocationProperties: GeolocationProperties,
    private val geoLocationService: GeolocationService
) : WebFilter {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
    ): Mono<Void> = mono {
        val request = exchange.request
        val method = request.method
        val path = request.uri.path
        val origin = request.headers.origin
        val originString = origin?.let { " with origin $origin" } ?: ""

        val ipAddress = exchange.request.getClientIp(geolocationProperties.realIpHeader)
        val location = geoLocationService.getLocationOrNull(request)
        val locationString = location?.let { " (${location.city.names["en"]}, ${location.country.isoCode})" } ?: ""

        logger.debug { "Incoming request  - $method $path from $ipAddress$locationString$originString" }

        try {
            chain.filter(exchange).awaitSingleOrNull()
            val status = exchange.response.statusCode
            logger.debug { "Outgoing response - $method $path: $status" }
        } catch (error: Throwable) {
            val status = if (error is ResponseStatusException) {
                error.statusCode
            } else {
                exchange.response.statusCode ?: "UNKNOWN"
            }
            logger.warn { "Request failed    - $method $path: $status - Error: ${error.message}" }
            throw error
        }

        return@mono null
    }
}
