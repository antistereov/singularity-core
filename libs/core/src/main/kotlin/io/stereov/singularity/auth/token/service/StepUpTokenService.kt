package io.stereov.singularity.auth.token.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.jwt.exception.TokenCreationException
import io.stereov.singularity.auth.jwt.exception.TokenExtractionException
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.token.component.TokenValueExtractor
import io.stereov.singularity.auth.token.exception.StepUpTokenCreationException
import io.stereov.singularity.auth.token.exception.StepUpTokenExtractionException
import io.stereov.singularity.auth.token.model.SessionTokenType
import io.stereov.singularity.auth.token.model.StepUpToken
import io.stereov.singularity.global.util.Constants
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.time.Instant
import java.util.*

/**
 * Service responsible for creating, validating, and managing step-up tokens for users.
 * Step-up tokens are used to confirm additional levels of authentication during a user session.
 *
 * This service uses JWT (JSON Web EmailVerificationTokenCreation) for encoding and decoding tokens, managing expiration,
 * and securely associating tokens with specific user IDs and session details.
 */
@Service
class StepUpTokenService(
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties,
    private val tokenValueExtractor: TokenValueExtractor,
) {

    private val logger = KotlinLogging.logger {}
    private val tokenType = SessionTokenType.StepUp

    /**
     * Creates a [StepUpToken] for a given user and session. The method generates and encodes
     * the token using specific claims and the provided timestamps.
     *
     * @param userId The unique identifier of the user for whom the step-up token is being created.
     * @param sessionId The unique identifier for the session associated with the user.
     * @param issuedAt The timestamp at which the token is issued. Defaults to the current instant if not provided.
     * @return A [Result] containing the created [StepUpToken] on success or a [StepUpTokenCreationException] if an error occurs during token creation.
     */
    suspend fun create(
        userId: ObjectId,
        sessionId: UUID,
        issuedAt: Instant = Instant.now()
    ): Result<StepUpToken, StepUpTokenCreationException> = coroutineBinding {
        logger.debug { "Creating step up token" }

        val claims = runCatching {
            JwtClaimsSet.builder()
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))
                .subject(userId.toHexString())
                .claim(Constants.JWT_SESSION_CLAIM, sessionId)
                .build()
        }
            .mapError { ex -> StepUpTokenCreationException.Encoding("Failed to build claim set for password reset token: ${ex.message}", ex)  }
            .bind()

        jwtService.encodeJwt(claims, tokenType.cookieName)
            .mapError { ex -> when (ex) {
                is TokenCreationException.Encoding -> StepUpTokenCreationException.Encoding("Failed to encode step-up token: ${ex.message}", ex)
                is TokenCreationException.Secret -> StepUpTokenCreationException.Secret("Failed to encode step-up token because of an exception related to secrets: ${ex.message}", ex)
            } }
            .map { jwt ->
                StepUpToken(userId, sessionId, jwt)
            }
            .bind()
    }

    /**
     * Creates a [StepUpToken] specifically for recovery purposes. This method ensures that it is called
     * only when the request originates from the predefined endpoint `/api/auth/2fa/totp/recover`.
     * It delegates the actual token creation to the [create] method after performing this validation.
     *
     * @param userId The ID of the user for whom the step-up token is being created.
     * @param sessionId The session ID associated with the user's session.
     * @param exchange The server web exchange containing request details.
     * @param issuedAt The timestamp indicating when the token was issued. Defaults to the current time if not specified.
     * @return A [Result] containing the created [StepUpToken] or a [StepUpTokenCreationException] in case of failure.
     */
    suspend fun createForRecovery(userId: ObjectId, sessionId: UUID, exchange: ServerWebExchange, issuedAt: Instant = Instant.now()): Result<StepUpToken, StepUpTokenCreationException> {
        logger.debug { "Creating step up token" }

        if (exchange.request.path.toString() != "/api/auth/2fa/totp/recover")
            return Err(StepUpTokenCreationException.Forbidden("Cannot createGroup step up token. This function call is only allowed when it is called from /api/auth/2fa/totp/recover"))

        return create(userId, sessionId, issuedAt)
    }

    /**
     * Extracts a [StepUpToken] from the provided token value, validates its contents against the
     * current user ID and session ID, and returns the corresponding [StepUpToken] if valid.
     *
     * @param tokenValue The JWT token value to be extracted and validated.
     * @param currentUserId The ID of the currently authenticated user.
     * @param currentSessionId The ID of the current session associated with the user.
     * @return A [Result] that contains the valid [StepUpToken] if extraction and validation succeed
     *  or a [StepUpTokenExtractionException] if any error occurs during the process.
     */
    suspend fun extract(
        tokenValue: String,
        currentUserId: ObjectId,
        currentSessionId: UUID
    ): Result<StepUpToken, StepUpTokenExtractionException> {
        logger.debug { "Extracting step up token" }

        return jwtService.decodeJwt(tokenValue, tokenType.cookieName)
            .mapError { ex -> StepUpTokenExtractionException.fromTokenExtractionException(ex) }
            .andThen { jwt ->
                val userId = jwt.subject
                    ?.let { ObjectId(it) }
                    ?: return@andThen Err(StepUpTokenExtractionException.Invalid("JWT does not contain sub"))

                if (userId != currentUserId) {
                    return@andThen Err(StepUpTokenExtractionException.Invalid("Step up token is not valid for currently logged in user"))
                }

                val sessionId = (jwt.claims[Constants.JWT_SESSION_CLAIM] as? String)
                    ?.let { UUID.fromString(it) }
                    ?: return@andThen Err(StepUpTokenExtractionException.Invalid("JWT does not contain session id"))

                if (sessionId != currentSessionId) {
                    return@andThen Err(StepUpTokenExtractionException.Invalid("Step up token is not valid for current session"))
                }

                return@andThen Ok(StepUpToken(userId, sessionId, jwt))
        }
    }

    /**
     * Extracts a [StepUpToken] token from the provided server exchange, validates its contents against
     * the current user ID and session ID, and returns the corresponding [StepUpToken] if valid.
     *
     * @param exchange The [ServerWebExchange] that contains the request and potential token information.
     * @param currentUserId The ID of the currently authenticated user.
     * @param currentSessionId The ID of the current session associated with the user.
     * @return A [Result] that contains the valid [StepUpToken] if extraction and validation succeed
     * or a [StepUpTokenExtractionException] if any error occurs during the process.
     */
    suspend fun extract(
        exchange: ServerWebExchange,
        currentUserId: ObjectId,
        currentSessionId: UUID
    ): Result<StepUpToken, StepUpTokenExtractionException> {
        return tokenValueExtractor.extractValue(exchange, tokenType).mapBoth(
            success = { tokenValue -> extract(tokenValue, currentUserId, currentSessionId) },
            failure = { ex ->
                when(ex) {
                    is TokenExtractionException.Missing -> Err(StepUpTokenExtractionException.Missing("No step-up token found in exchange: ${ex.message}",ex))
                }
            })
    }
}
