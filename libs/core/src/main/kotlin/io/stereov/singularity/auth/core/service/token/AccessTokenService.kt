package io.stereov.singularity.auth.core.service.token

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toResultOr
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.component.TokenValueExtractor
import io.stereov.singularity.auth.core.exception.AccessTokenCreationException
import io.stereov.singularity.auth.core.exception.AccessTokenExtractionException
import io.stereov.singularity.auth.core.model.token.AccessToken
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.auth.jwt.exception.TokenCreationException
import io.stereov.singularity.auth.jwt.exception.TokenExtractionException
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.oauth2.exception.model.OAuth2FlowException
import io.stereov.singularity.auth.oauth2.model.OAuth2ErrorCode
import io.stereov.singularity.global.util.Constants
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.user.core.model.Role
import io.stereov.singularity.user.core.model.UserDocument
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.time.Instant
import java.util.*

/**
 * Service class responsible for handling the creation, extraction, and validation
 * of access tokens used for user authentication and session management.
 *
 * @property jwtService Service for encoding and decoding JWT tokens.
 * @property accessTokenCache Cache to manage access token-related data.
 * @property jwtProperties Configuration properties for JWT tokens, including expiration time.
 * @property tokenValueExtractor Utility to extract token values from web exchanges.
 */
@Service
class AccessTokenService(
    private val jwtService: JwtService,
    private val accessTokenCache: AccessTokenCache,
    private val jwtProperties: JwtProperties,
    private val tokenValueExtractor: TokenValueExtractor,
) {

    private val logger = KotlinLogging.logger {}
    private val tokenType = SessionTokenType.Access

    /**
     * Creates an access token for a specified user and session.
     *
     * @param user The user document containing the user's information, including ID, roles, and groups.
     * @param sessionId The identifier of the session for which the token is being created.
     * @param issuedAt The timestamp representing when the token is issued. Defaults to the current time.
     * @return A [Result] containing either the created [AccessToken] or an [AccessTokenCreationException] in case of a failure.
     */
    suspend fun create(
        user: UserDocument,
        sessionId: UUID,
        issuedAt: Instant = Instant.now()
    ): Result<AccessToken, AccessTokenCreationException> = coroutineBinding {
        logger.debug { "Creating access token for user ${user.id} and session $sessionId" }

        val tokenId = Random.generateString(20)
            .mapError { ex -> AccessTokenCreationException.Failed("Failed to generate token id: ${ex.message}", ex) }
            .bind()

        runCatching { accessTokenCache.allowTokenId(user.id, sessionId, tokenId) }
            .mapError { ex -> AccessTokenCreationException.Cache("Failed to cache access token: ${ex.message}", ex) }
            .bind()

        val claims = runCatching {
            JwtClaimsSet.builder()
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))
                .subject(user.id.toHexString())
                .claim(Constants.JWT_ROLES_CLAIM, user.roles)
                .claim(Constants.JWT_SESSION_CLAIM, sessionId)
                .claim(Constants.JWT_GROUPS_CLAIM, user.groups)
                .id(tokenId)
                .build()
        }
            .mapError { ex -> AccessTokenCreationException.Encoding("Failed to build claim set: ${ex.message}", ex) }
            .bind()

        jwtService.encodeJwt(claims, tokenType.cookieName)
            .mapError { ex -> when(ex) {
                is TokenCreationException.Encoding -> AccessTokenCreationException.Encoding("Failed to encode access token: ${ex.message}", ex)
                is TokenCreationException.Secret -> AccessTokenCreationException.Secret("Failed to fetch current JWT secret: ${ex.message}", ex)
            } }
            .map { jwt ->
                AccessToken(user.id, sessionId, tokenId, user.roles, user.groups, jwt)
            }
            .bind()
    }

    /**
     * Extracts an access token from the given server web exchange.
     *
     * @param exchange the server web exchange from which to extract the access token
     * @return a [Result] containing the extracted [AccessToken] on success,
     *   or an [AccessTokenExtractionException] on failure
     */
    suspend fun extract(exchange: ServerWebExchange): Result<AccessToken, AccessTokenExtractionException> {

        return tokenValueExtractor.extractValue(exchange, tokenType, true)
            .mapBoth(
                success = { tokenValue -> extract(tokenValue) },
                failure = { ex ->
                    when (ex) {
                        is TokenExtractionException.Missing -> Err(AccessTokenExtractionException.Missing("No access token was found in exchange: ${ex.message}", ex))
                    }
                }
            )
    }

    suspend fun extractOrOAuth2FlowException(exchange: ServerWebExchange): AccessToken {

        return runCatching { extractOrNull(exchange) }
            .getOrElse { exception ->
                when (exception) {
                    is TokenExpiredException -> throw OAuth2FlowException(
                        OAuth2ErrorCode.ACCESS_TOKEN_EXPIRED,
                        "Failed to connect a new provider to the current user. AccessToken expired.",
                        exception
                    )

                    else -> throw OAuth2FlowException(
                        OAuth2ErrorCode.INVALID_ACCESS_TOKEN,
                        "Failed to connect a new provider to the current user: AccessToken is invalid",
                        exception
                    )
                }
            } ?: throw OAuth2FlowException(
            OAuth2ErrorCode.ACCESS_TOKEN_MISSING,
            "Failed to connect a new provider to the current user: AccessToken is invalid"
        )
    }

    /**
     * Extracts and validates an access token from the provided token value.
     *
     * @param tokenValue The raw access token string to be decoded and validated.
     * @return A [Result] containing the decoded [AccessToken] if successful, or an [AccessTokenExtractionException]
     *         if the extraction or validation process fails.
     */
    suspend fun extract(tokenValue: String): Result<AccessToken, AccessTokenExtractionException> {
        logger.debug { "Extracting and validating access token" }

        return jwtService.decodeJwt(tokenValue, tokenType.cookieName)
            .mapError { ex -> when(ex) {
                is TokenExtractionException.Missing -> AccessTokenExtractionException.Missing("Access token missing: ${ex.message}", ex)
                is TokenExtractionException.Invalid -> AccessTokenExtractionException.Invalid("Access token invalid: ${ex.message}", ex)
                is TokenExtractionException.Expired -> AccessTokenExtractionException.Expired("Access token expired: ${ex.message}", ex)
            } }
            .andThen { jwt ->
                coroutineBinding {
                    val userId = jwt.subject
                        .toResultOr { AccessTokenExtractionException.Invalid("Access token does not contain sub") }
                        .andThen { sub ->
                            runCatching { ObjectId(sub) }
                                .mapError { ex -> AccessTokenExtractionException.Invalid("Invalid ObjectId in sub: $sub", ex) }
                        }.bind()

                    val sessionId = (jwt.claims[Constants.JWT_SESSION_CLAIM] as? String)
                        .toResultOr { AccessTokenExtractionException.Invalid("Access token does not contain session id") }
                        .andThen { s ->
                            runCatching { UUID.fromString(s) }.mapError { ex ->
                                AccessTokenExtractionException.Invalid("Invalid session id: $s", ex)
                            }
                        }.bind()

                    val tokenId = jwt.id
                        .toResultOr { AccessTokenExtractionException.Invalid("Access token does not contain token id") }
                        .bind()

                    val roles = (jwt.claims[Constants.JWT_ROLES_CLAIM] as? List<*>)
                        .toResultOr { AccessTokenExtractionException.Invalid("Access token does not contain roles") }
                        .andThen { list ->
                            val parsed = mutableSetOf<Role>()
                            for (raw in list) {
                                val s = (raw as? String)
                                    .toResultOr { AccessTokenExtractionException.Invalid("Cannot decode role $raw") }
                                    .bind()
                                val r = Role.fromString(s)
                                    .toResultOr { AccessTokenExtractionException.Invalid("Unknown role $s") }
                                    .bind()
                                parsed += r
                            }
                            Ok(parsed.toSet())
                        }.bind()

                    val groups = (jwt.claims[Constants.JWT_GROUPS_CLAIM] as? List<*>)
                        .toResultOr { AccessTokenExtractionException.Invalid("Access token does not contain groups") }
                        .andThen { list ->
                            val parsed = mutableSetOf<String>()
                            for (raw in list) {
                                val g = (raw as? String)
                                    .toResultOr { AccessTokenExtractionException.Invalid("Cannot decode group $raw") }
                                    .bind()
                                parsed += g
                            }
                            Ok(parsed.toSet())
                        }.bind()

                accessTokenCache.isTokenIdValid(userId, sessionId, tokenId)
                    .mapError { ex -> AccessTokenExtractionException.Cache("Failed to access access token allowlist: ${ex.message}", ex) }
                    .andThen { valid ->
                        if (valid) {
                            Ok(AccessToken(userId, sessionId, tokenId, roles, groups, jwt))
                        } else {
                            Err(AccessTokenExtractionException.Invalid("Access token is not valid"))
                        }
                    }
                    .bind()
            }
        }
    }
}
