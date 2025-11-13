package io.stereov.singularity.auth.core.service.token

import com.github.michaelbull.result.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.component.TokenValueExtractor
import io.stereov.singularity.auth.core.exception.StepUpTokenCreationException
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.auth.core.model.token.StepUpToken
import io.stereov.singularity.auth.jwt.exception.TokenCreationException
import io.stereov.singularity.auth.jwt.exception.TokenExtractionException
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.global.util.Constants
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.time.Instant
import java.util.*

@Service
class StepUpTokenService(
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties,
    private val tokenValueExtractor: TokenValueExtractor,
) {

    private val logger = KotlinLogging.logger {}
    private val tokenType = SessionTokenType.StepUp

    suspend fun create(userId: ObjectId, sessionId: UUID, issuedAt: Instant = Instant.now()): Result<StepUpToken, StepUpTokenCreationException.Encoding> {
        logger.debug { "Creating step up token" }

        val claims = JwtClaimsSet.builder()
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))
            .subject(userId.toHexString())
            .claim(Constants.JWT_SESSION_CLAIM, sessionId)
            .build()

        return jwtService.encodeJwt(claims, tokenType.cookieName)
            .mapError { ex -> when (ex) {
                is TokenCreationException.Encoding -> StepUpTokenCreationException.Encoding("Failed to encode step-up token: ${ex.msg}", ex)
            } }
            .map { jwt ->
                StepUpToken(userId, sessionId, jwt)
            }
    }

    /**
     * Create a [StepUpToken] for recovery. This method can only be called from the recovery path.
     */
    suspend fun createForRecovery(userId: ObjectId, sessionId: UUID, exchange: ServerWebExchange, issuedAt: Instant = Instant.now()): Result<StepUpToken, StepUpTokenCreationException> {
        logger.debug { "Creating step up token" }

        if (exchange.request.path.toString() != "/api/auth/2fa/totp/recover")
            return Err(StepUpTokenCreationException.Forbidden("Cannot createGroup step up token. This function call is only allowed when it is called from /api/auth/2fa/totp/recover"))

        return create(userId, sessionId, issuedAt)
    }

    suspend fun extract(
        tokenValue: String,
        currentUserId: ObjectId,
        currentSessionId: UUID
    ): Result<StepUpToken, TokenExtractionException> {
        logger.debug { "Extracting step up token" }

        return jwtService.decodeJwt(tokenValue, tokenType.cookieName)
            .flatMap { jwt ->
                val userId = jwt.subject
                    ?.let { ObjectId(it) }
                    ?: return@flatMap Err(TokenExtractionException.Invalid("JWT does not contain sub"))

                if (userId != currentUserId) {
                    return@flatMap Err(TokenExtractionException.Invalid("Step up token is not valid for currently logged in user"))
                }

                val sessionId = (jwt.claims[Constants.JWT_SESSION_CLAIM] as? String)
                    ?.let { UUID.fromString(it) }
                    ?: return@flatMap Err(TokenExtractionException.Invalid("JWT does not contain session id"))

                if (sessionId != currentSessionId) {
                    return@flatMap Err(TokenExtractionException.Invalid("Step up token is not valid for current session"))
                }

                return@flatMap Ok(StepUpToken(userId, sessionId, jwt))
        }
    }

    suspend fun extract(
        exchange: ServerWebExchange,
        currentUserId: ObjectId,
        currentSessionId: UUID
    ): Result<StepUpToken, TokenExtractionException> {
        return tokenValueExtractor.extractValue(exchange, tokenType).mapBoth(
            success = { tokenValue -> extract(tokenValue, currentUserId, currentSessionId) },
            failure = { ex ->
                when(ex) {
                    is TokenExtractionException.Missing -> Err(TokenExtractionException.Missing("No step-up token found in exchange: ${ex.message}",ex))
                }
            })
    }
}
