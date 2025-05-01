package io.stereov.singularity.core.filter

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * # Filter for logging incoming requests and outgoing responses.
 *
 * This filter logs the details of incoming requests and outgoing responses,
 * including the HTTP method, path, and status code.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
class LoggingFilter : WebFilter {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {

        val request = exchange.request
        val method = request.method
        val path = request.uri.path
        val ipAddress = request.remoteAddress?.address?.hostAddress

        logger.debug { "Incoming request  - $method $path" }

        return chain.filter(exchange)
            .doOnSuccess {

                val status = exchange.response.statusCode
                logger.debug { "Outgoing response - $method $path: $status from $ipAddress" }
            }
            .onErrorResume { error ->
                val status = if (error is ResponseStatusException) {
                    error.statusCode
                } else {
                    exchange.response.statusCode ?: "UNKNOWN"
                }

                logger.warn { "Request failed    - $method $path: $status - Error: ${error.message}" }
                throw error
            }
    }
}
