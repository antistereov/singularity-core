package io.stereov.singularity.auth.token.component

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.jwt.exception.TokenExtractionException
import io.stereov.singularity.auth.token.model.SecurityTokenType
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
     * Extracts the value of a security token from the request, based on cookies and headers.
     *
     * The token can be retrieved from a cookie or a header, depending on the configuration provided
     * in the [AuthProperties]. If header authentication is disallowed, only the cookie is checked.
     * If "Bearer" prefix is expected, it is removed from the header token before returning.
     *
     * @param exchange the server web exchange that contains the request to extract the token from.
     * @param securityTokenType defines the type of security token, including associated cookie name and header name.
     * @param useBearerPrefix a flag indicating whether to look for and remove the "Bearer" prefix in the header token.
     * @return a [Result] containing the extracted token value as [String] if present, or [TokenExtractionException.Missing]
     *         if no valid token could be found.
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
