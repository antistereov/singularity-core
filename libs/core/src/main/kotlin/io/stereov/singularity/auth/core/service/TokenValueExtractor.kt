package io.stereov.singularity.auth.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.model.SecurityTokenType
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange

@Service
class TokenValueExtractor(
    private val authProperties: AuthProperties
) {

    private val logger = KotlinLogging.logger {}

    fun extractValueOrNull(exchange: ServerWebExchange, securityTokenType: SecurityTokenType, useBearerPrefix: Boolean = false): String? {
        logger.debug { "Extracting ${securityTokenType.cookieName} from request" }

        val cookieToken = exchange.request.cookies[securityTokenType.cookieName]?.firstOrNull()?.value

        if (!authProperties.allowHeaderAuthentication) return cookieToken

        val headerToken = if (useBearerPrefix) {
            exchange.request.headers.getFirst(securityTokenType.header)
                ?.takeIf { it.startsWith("Bearer ") }
                ?.removePrefix("Bearer ")
        } else {
            exchange.request.headers.getFirst(securityTokenType.header)
        }

        return if (authProperties.preferHeaderAuthentication) {
            headerToken ?: cookieToken
        } else {
            cookieToken ?: headerToken
        }
    }

    fun extractValue(exchange: ServerWebExchange, securityTokenType: SecurityTokenType, useBearerPrefix: Boolean = false): String {
        return extractValueOrNull(exchange, securityTokenType, useBearerPrefix)
            ?: throw InvalidTokenException("No ${securityTokenType.cookieName} found")
    }

}
