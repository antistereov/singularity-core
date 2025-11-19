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
 * LoggingFilter is an implementation of [WebFilter] responsible for logging incoming HTTP requests
 * and outgoing HTTP responses within a reactive application. It provides detailed information
 * about the request/response process, including the HTTP method, URI, client IP address, and
 * geolocation details if available.
 *
 * The filter uses the `geolocationProperties` and `geoLocationService` to optionally retrieve
 * geolocation data associated with the client IP address. This data can help provide more
 * contextual logging based on the location of the request origin.
 *
 * Error handling is integrated to log exceptions or failed responses appropriately, aiding in
 * debugging and tracing issues in the request-response lifecycle.
 *
 * @constructor
 * Initializes the LoggingFilter with required dependencies for geolocation and logging.
 *
 * @param geolocationProperties The configuration properties for geolocation functionality.
 * @param geoLocationService The service used to retrieve geolocation data.
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
