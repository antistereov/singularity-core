package io.stereov.web.filter

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class LoggingFilter : WebFilter {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {

        val request = exchange.request
        val method = request.method
        val path = request.uri.path

        logger.debug { "Incoming request  - $method $path" }

        return chain.filter(exchange)
            .doOnSuccess {

                val status = exchange.response.statusCode
                logger.debug { "Outgoing response - $method $path: $status" }
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
