package io.stereov.singularity.auth.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.model.TokenType
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange

@Service
class TokenValueExtractor(
    private val authProperties: AuthProperties
) {

    private val logger = KotlinLogging.logger {}

    fun extractValueOrNull(exchange: ServerWebExchange, tokenType: TokenType, useBearerPrefix: Boolean = false): String? {
        logger.debug { "Extracting ${tokenType.cookieKey} from request" }

        val cookieToken = exchange.request.cookies[tokenType.cookieKey]?.firstOrNull()?.value

        if (!authProperties.allowHeaderAuthentication) return cookieToken

        val headerToken = if (useBearerPrefix) {
            exchange.request.headers.getFirst(tokenType.header)
                ?.takeIf { it.startsWith("Bearer ") }
                ?.removePrefix("Bearer ")
        } else {
            exchange.request.headers.getFirst(tokenType.header)
        }

        return if (authProperties.preferHeaderAuthentication) {
            headerToken ?: cookieToken
        } else {
            cookieToken ?: headerToken
        }
    }

    fun extractValue(exchange: ServerWebExchange, tokenType: TokenType, useBearerPrefix: Boolean = false): String {
        return extractValueOrNull(exchange, tokenType, useBearerPrefix)
            ?: throw InvalidTokenException("No ${tokenType.cookieKey} found")
    }

}
