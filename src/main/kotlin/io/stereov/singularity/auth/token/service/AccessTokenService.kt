package io.stereov.singularity.auth.token.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.auth.jwt.exception.TokenCreationException
import io.stereov.singularity.auth.jwt.exception.TokenExtractionException
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.oauth2.exception.OAuth2FlowException
import io.stereov.singularity.auth.oauth2.model.OAuth2ErrorCode
import io.stereov.singularity.auth.token.component.TokenValueExtractor
import io.stereov.singularity.auth.token.exception.AccessTokenCreationException
import io.stereov.singularity.auth.token.exception.AccessTokenExtractionException
import io.stereov.singularity.auth.token.model.AccessToken
import io.stereov.singularity.auth.token.model.SessionTokenType
import io.stereov.singularity.global.util.Constants
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.principal.core.model.Principal
import io.stereov.singularity.principal.core.model.Role
import io.stereov.singularity.principal.core.model.sensitve.SensitivePrincipalData
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
     * @param principal The user document containing the user's information, including ID, roles, and groups.
     * @param sessionId The identifier of the session for which the token is being created.
     * @param issuedAt The timestamp representing when the token is issued. Defaults to the current time.
     * @return A [Result] containing either the created [AccessToken] or an [AccessTokenCreationException] in case of a failure.
     */
    suspend fun create(
        principal: Principal<out Role, out SensitivePrincipalData>,
        sessionId: UUID,
        issuedAt: Instant = Instant.now()
    ): Result<AccessToken, AccessTokenCreationException> = coroutineBinding {
        val id = principal.id
            .mapError { ex -> AccessTokenCreationException.InvalidPrincipal("Failed to generate access token because the associated principal document contains no ID: ${ex.message}", ex) }
            .bind()
        logger.debug { "Creating access token for user ${principal._id} and session $sessionId" }

        val tokenId = Random.generateString(20)
            .mapError { ex -> AccessTokenCreationException.Failed("Failed to generate token id: ${ex.message}", ex) }
            .bind()

        runCatching { accessTokenCache.allowTokenId(id, sessionId, tokenId) }
            .mapError { ex -> AccessTokenCreationException.Cache("Failed to cache access token: ${ex.message}", ex) }
            .bind()

        val claims = runCatching {
            JwtClaimsSet.builder()
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))
                .subject(id.toHexString())
                .claim(Constants.JWT_ROLES_CLAIM, principal.roles)
                .claim(Constants.JWT_SESSION_CLAIM, sessionId)
                .claim(Constants.JWT_GROUPS_CLAIM, principal.groups)
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
                AccessToken(id, sessionId, tokenId, principal.roles, principal.groups, jwt)
            }
            .bind()
    }

    /**
     * Extracts authentication information from the provided server web exchange.
     *
     * @param exchange the server web exchange from which to extract the information
     * @return a [Result] wrapping either an [AuthenticationOutcome] on success or an [AccessTokenExtractionException] on failure
     */
    suspend fun extract(exchange: ServerWebExchange): Result<AuthenticationOutcome, AccessTokenExtractionException> {

        return tokenValueExtractor.extractValue(exchange, tokenType, true)
            .flatMapEither(
                success = { tokenValue -> extract(tokenValue) },
                failure = { ex -> when (ex) {
                    is TokenExtractionException.Missing -> Ok(AuthenticationOutcome.None())
                } }
            )
    }

    /**
     * Extracts authentication from the provided ServerWebExchange or throws an OAuth2FlowException
     * if the process encounters any errors such as expired or invalid access tokens.
     *
     * @param exchange the ServerWebExchange containing the request and response information
     * @return the extracted authentication outcome if successful
     * @throws OAuth2FlowException if the access token is expired, invalid, or missing
     */
    suspend fun extractOrOAuth2FlowException(exchange: ServerWebExchange): AuthenticationOutcome.Authenticated {

        val authenticationOutcome =  extract(exchange)
            .getOrElse { exception ->
                when (exception) {
                    is AccessTokenExtractionException.Expired -> throw OAuth2FlowException(
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
            }
            return when (authenticationOutcome) {
                is AuthenticationOutcome.Authenticated -> authenticationOutcome
                is AuthenticationOutcome.None -> throw OAuth2FlowException(OAuth2ErrorCode.ACCESS_TOKEN_MISSING, "Failed to connect a new provider to the current user: AccessToken is invalid")
            }
    }

    /**
     * Extracts and validates the provided access token value, performing authentication checks and returning an authentication outcome.
     *
     * @param tokenValue the JWT token value to be extracted and validated.
     * @return a [Result] containing either the authentication outcome ([AuthenticationOutcome]) if the token is valid,
     *  or an [AccessTokenExtractionException] if the extraction fails.
     */
    suspend fun extract(tokenValue: String): Result<AuthenticationOutcome, AccessTokenExtractionException> {
        logger.debug { "Extracting and validating access token" }

        return jwtService.decodeJwt(tokenValue, tokenType.cookieName)
            .flatMapEither(
                success = { jwt -> coroutineBinding {
                    val userId = jwt.subject
                        .toResultOr { AccessTokenExtractionException.Invalid("Access token does not contain sub") }
                        .andThen { sub ->
                            runCatching { ObjectId(sub) }
                                .mapError { ex ->
                                    AccessTokenExtractionException.Invalid(
                                        "Invalid ObjectId in sub: $sub",
                                        ex
                                    )
                                }
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
                                    .mapError { ex -> AccessTokenExtractionException.Invalid("Unknown role $s: ${ex.message}", ex) }
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
                        .mapError { ex ->
                            AccessTokenExtractionException.Cache(
                                "Failed to access access token allowlist: ${ex.message}",
                                ex
                            )
                        }
                        .andThen { onAllowlist ->
                            if (onAllowlist) {
                                Ok(AuthenticationOutcome.Authenticated(userId, roles, groups, AccessToken(userId, sessionId, tokenId, roles, groups, jwt) ))
                            } else {
                                Err(AccessTokenExtractionException.Expired("Access token is expired"))
                            }
                        }
                        .bind()
                } },
                failure = {ex -> when (ex) {
                    is TokenExtractionException.Invalid -> Err(AccessTokenExtractionException.Invalid("Access token invalid: ${ex.message}", ex))
                    is TokenExtractionException.Expired -> Err(AccessTokenExtractionException.Expired("Access token expired: ${ex.message}", ex))
                    is TokenExtractionException.Missing -> Ok(AuthenticationOutcome.None())
                } }
            )
    }
}
