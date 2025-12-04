package io.stereov.singularity.auth.token.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.token.component.TokenValueExtractor
import io.stereov.singularity.auth.token.exception.TwoFactorAuthenticationTokenCreationException
import io.stereov.singularity.auth.token.exception.TwoFactorAuthenticationTokenExtractionException
import io.stereov.singularity.auth.token.model.TwoFactorAuthenticationToken
import io.stereov.singularity.auth.token.model.TwoFactorTokenType
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.time.Instant

/**
 * Service responsible for managing two-factor authentication (2FA) tokens.
 *
 * This service provides methods to create and extract two-factor authentication tokens. It integrates
 * with JWT-based encoding/decoding mechanisms and utilizes token value extraction logic for validation.
 */
@Service
class TwoFactorAuthenticationTokenService(
    private val jwtProperties: JwtProperties,
    private val jwtService: JwtService,
    private val tokenValueExtractor: TokenValueExtractor
) {

    private val logger = KotlinLogging.logger {}
    private val tokenType = TwoFactorTokenType.Authentication

    /**
     * Creates a two-factor authentication token for a user.
     *
     * This method generates a JWT token with claims such as issuedAt, expiration, and userId as the subject,
     * then encodes the token. If any step in the process fails, it will return an exception wrapped in a [Result].
     *
     * @param userId the [ObjectId] of the user for whom the two-factor authentication token is being created
     * @param issuedAt the [Instant] representing the time at which the token is issued; defaults to the current time
     * @return a [Result] containing either the generated [TwoFactorAuthenticationToken] or a
     * [TwoFactorAuthenticationTokenCreationException] in case of failure
     */
    suspend fun create(
        userId: ObjectId, 
        issuedAt: Instant = Instant.now()
    ): Result<TwoFactorAuthenticationToken, TwoFactorAuthenticationTokenCreationException> = coroutineBinding {
        logger.debug { "Creating two factor token" }

        val claims = runCatching {
            JwtClaimsSet.builder()
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))
                .subject(userId.toHexString())
                .build()
        }
            .mapError { ex -> TwoFactorAuthenticationTokenCreationException.Encoding("Failed to build claim set: ${ex.message}", ex) }
            .bind()

        jwtService.encodeJwt(claims, tokenType.cookieName)
            .mapError { ex -> TwoFactorAuthenticationTokenCreationException.fromTokenCreationException(ex) }
            .map { jwt -> TwoFactorAuthenticationToken(userId, jwt) }
            .bind()
    }

    /**
     * Extracts a [TwoFactorAuthenticationToken] from the provided [ServerWebExchange].
     *
     * This function retrieves the token value from the exchange using the configured token type, and then attempts
     * to decode and validate the token. If the extraction or validation process fails, it will return an exception
     * wrapped in a [Result].
     *
     * @param exchange the [ServerWebExchange] containing the request from which the token value is to be extracted
     * @return a [Result] containing either the extracted [TwoFactorAuthenticationToken]
     * or a [TwoFactorAuthenticationTokenExtractionException] in case of failure
     */
    suspend fun extract(exchange: ServerWebExchange): Result<TwoFactorAuthenticationToken, TwoFactorAuthenticationTokenExtractionException> {
        return tokenValueExtractor.extractValue(exchange, tokenType)
            .mapError { ex -> TwoFactorAuthenticationTokenExtractionException.fromTokenExtractionException(ex) }
            .andThen { tokenValue -> extract(tokenValue)}
    }

    /**
     * Extracts a [TwoFactorAuthenticationToken] from the provided token value.
     *
     * This function decodes the JWT token, validates its subject, and constructs a TwoFactorAuthenticationToken object
     * if successful. If the process fails at any step, it will return an exception wrapped in a Result.
     *
     * @param tokenValue the raw token value to be decoded and validated
     * @return a [Result] containing either the extracted [TwoFactorAuthenticationToken]
     *  or a [TwoFactorAuthenticationTokenExtractionException] in case of failure
     */
    suspend fun extract(
        tokenValue: String
    ): Result<TwoFactorAuthenticationToken, TwoFactorAuthenticationTokenExtractionException> = coroutineBinding {
        logger.debug { "Extracting two factor login token" }

        val jwt = jwtService.decodeJwt(tokenValue, tokenType.cookieName)
            .mapError { ex -> TwoFactorAuthenticationTokenExtractionException.fromTokenExtractionException(ex) }
            .bind()

        val userId = jwt.subject
            .toResultOr { TwoFactorAuthenticationTokenExtractionException.Invalid("Access token does not contain sub") }
            .andThen { sub ->
                runCatching { ObjectId(sub) }
                    .mapError { ex ->
                        TwoFactorAuthenticationTokenExtractionException.Invalid(
                            "Invalid ObjectId in sub: $sub",
                            ex
                        )
                    }
            }.bind()

        TwoFactorAuthenticationToken(userId, jwt)
    }
}
