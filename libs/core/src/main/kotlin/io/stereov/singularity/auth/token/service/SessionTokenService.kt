package io.stereov.singularity.auth.token.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.jwt.exception.TokenCreationException
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.token.component.TokenValueExtractor
import io.stereov.singularity.auth.token.exception.SessionTokenExtractionException
import io.stereov.singularity.auth.token.model.SessionToken
import io.stereov.singularity.auth.token.model.SessionTokenType
import io.stereov.singularity.global.util.Constants
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.time.Instant
import java.util.*

@Service
class SessionTokenService(
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties,
    private val tokenValueExtractor: TokenValueExtractor
) {

    private val logger = KotlinLogging.logger {}
    val tokenType = "session"

    /**
     * Creates a session token with the provided session information and locale.
     *
     * @param sessionInfo An optional [SessionInfoRequest] containing details such as browser and operating system.
     * @param issuedAt The timestamp indicating when the token was issued. Defaults to the current instant.
     * @param locale An optional [Locale] representing the locale to be included in the token.
     * @return A [Result] containing a [SessionToken] if token creation is successful, or a [TokenCreationException] if an error occurs.
     */
    suspend fun create(
        sessionInfo: SessionInfoRequest? = null,
        issuedAt: Instant = Instant.now(),
        locale: Locale? = null
    ): Result<SessionToken, TokenCreationException> = coroutineBinding {
        logger.debug { "Creating session token" }

        val claims = runCatching {
            val claimsBuilder = JwtClaimsSet.builder()
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))

            if (sessionInfo?.browser != null) {
                claimsBuilder.claim(Constants.JWT_BROWSER_CLAIM, sessionInfo.browser)
            }
            if (sessionInfo?.os != null) {
                claimsBuilder.claim(Constants.JWT_OS_CLAIM, sessionInfo.os)
            }
            if (locale != null) {
                claimsBuilder.claim(Constants.JWT_LOCALE_CLAIM, locale.toLanguageTag())
            }

            claimsBuilder.build()
        }
            .mapError { ex -> TokenCreationException.Encoding("Failed to build claim set for session token: ${ex.message}", ex) }
            .bind()


        jwtService.encodeJwt(claims, tokenType)
            .map { jwt -> SessionToken(sessionInfo?.browser, sessionInfo?.os, locale, jwt) }
            .bind()
    }

    /**
     * Extracts a session token from the provided [ServerWebExchange].
     *
     * @param exchange The [ServerWebExchange] containing information to extract the session token.
     * @return A [Result] containing a [SessionToken] if extraction is successful,
     *  or a [SessionTokenExtractionException] if an error occurs during the process.
     */
    suspend fun extract(exchange: ServerWebExchange): Result<SessionToken, SessionTokenExtractionException> {
        return tokenValueExtractor.extractValue(exchange, SessionTokenType.Session)
            .mapError { ex -> SessionTokenExtractionException.fromTokenExtractionException(ex) }
            .andThen { tokenValue -> extract(tokenValue) }

    }

    /**
     * Extracts a session token from the provided tokenValue.
     *
     * @param tokenValue The raw token string to be decoded and extracted into a [SessionToken].
     * @return A [Result] containing a [SessionToken] if extraction is successful, or a [SessionTokenExtractionException] if an error occurs.
     */
    suspend fun extract(tokenValue: String): Result<SessionToken, SessionTokenExtractionException> = coroutineBinding {
        logger.debug { "Extracting session token" }

        val jwt = jwtService.decodeJwt(tokenValue, tokenType)
            .mapError { ex -> SessionTokenExtractionException.fromTokenExtractionException(ex) }
            .bind()
        
        val browser = runCatching { jwt.claims[Constants.JWT_BROWSER_CLAIM] as? String }
            .mapError { ex -> SessionTokenExtractionException.Invalid("Browser claim in session token is invalid: ${ex.message}", ex) }
            .bind()
        val os = runCatching { jwt.claims[Constants.JWT_OS_CLAIM] as? String }
            .mapError { ex -> SessionTokenExtractionException.Invalid("OS claim in session token is invalid: ${ex.message}", ex) }
            .bind()
        val locale = runCatching { 
            (jwt.claims[Constants.JWT_LOCALE_CLAIM] as? String)
                ?.let { runCatching { Locale.forLanguageTag(it) }.getOrElse { null } }
        }
            .mapError { ex -> SessionTokenExtractionException.Invalid("locale claim in session token is invalid: ${ex.message}", ex) }
            .bind()

        SessionToken(browser, os, locale, jwt)
    }
}