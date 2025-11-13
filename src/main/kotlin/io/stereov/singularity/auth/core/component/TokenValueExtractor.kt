package io.stereov.singularity.auth.core.component

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.model.token.SecurityTokenType
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.jwt.exception.TokenExtractionException
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange

/**
 * Helper class to extract a token value from a request.
 *
 * It is based on the [AuthProperties] and whether header authentication is allowed or preferred.
 */
@Component
class TokenValueExtractor(
    private val authProperties: AuthProperties
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Extracts the value of a token from the [ServerWebExchange] either from a cookie or a header.
     *
     * Based on [AuthProperties] it can allow header values and prefer header values to cookie values.
     */
    fun extractValue(
        exchange: ServerWebExchange,
        securityTokenType: SecurityTokenType,
        useBearerPrefix: Boolean = false
    ): Result<String, TokenExtractionException.Missing> {
        logger.debug { "Extracting ${securityTokenType.cookieName} from request" }

        val cookieToken = exchange.request.cookies[securityTokenType.cookieName]?.firstOrNull()?.value

        if (!authProperties.allowHeaderAuthentication) {
            return cookieToken
                ?.let { Ok(it) }
                ?: Err(TokenExtractionException.Missing("No token of type ${securityTokenType.header} found in exchange cookies and header authentication is forbidden"))
        }

        val headerToken = if (useBearerPrefix) {
            exchange.request.headers.getFirst(securityTokenType.header)
                ?.takeIf { it.startsWith("Bearer ") }
                ?.removePrefix("Bearer ")
        } else {
            exchange.request.headers.getFirst(securityTokenType.header)
        }

        val token = if (authProperties.preferHeaderAuthentication) {
            headerToken ?: cookieToken
        } else {
            cookieToken ?: headerToken
        }

        return token
            ?.let { Ok(it) }
            ?: Err(TokenExtractionException.Missing("No token of type ${securityTokenType.header} found in exchange"))
    }
}
