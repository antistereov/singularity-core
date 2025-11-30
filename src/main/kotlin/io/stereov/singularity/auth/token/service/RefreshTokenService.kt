package io.stereov.singularity.auth.token.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.model.SessionInfo
import io.stereov.singularity.auth.geolocation.properties.GeolocationProperties
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.token.component.TokenValueExtractor
import io.stereov.singularity.auth.token.exception.RefreshTokenCreationException
import io.stereov.singularity.auth.token.exception.RefreshTokenExtractionException
import io.stereov.singularity.auth.token.model.RefreshToken
import io.stereov.singularity.auth.token.model.SessionTokenType
import io.stereov.singularity.global.util.Constants
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.global.util.getClientIp
import io.stereov.singularity.principal.core.model.Principal
import io.stereov.singularity.principal.core.model.Role
import io.stereov.singularity.principal.core.model.sensitve.SensitivePrincipalData
import io.stereov.singularity.principal.core.service.PrincipalService
import io.stereov.singularity.principal.core.service.UserService
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.net.InetAddress
import java.time.Instant
import java.util.*

/**
 * Service responsible for managing refresh tokens in the application.
 *
 * This service handles the creation, extraction, and validation of refresh tokens associated with user sessions.
 * It integrates with various other components like [JwtService], [UserService], and [GeolocationService] to
 * ensure secure and efficient token lifecycle management.
 */
@Service
class RefreshTokenService(
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties,
    private val geolocationService: GeolocationService,
    private val geolocationProperties: GeolocationProperties,
    private val tokenValueExtractor: TokenValueExtractor,
    private val principalService: PrincipalService
) {

    private val logger = KotlinLogging.logger {}
    private val tokenType = SessionTokenType.Refresh

    /**
     * Creates a new [RefreshToken] for a user session and automatically updates the user sessions.
     *
     * @param principal The user document associated with the [RefreshToken].
     * @param sessionId The unique identifier of the user's session.
     * @param sessionInfo Optional session information about the user's browser and operating system.
     * @param exchange The server web exchange containing request context.
     *
     * @return A [Result] containing the created [RefreshToken] on success or a [RefreshTokenCreationException] on failure.
     */
    suspend fun create(
        principal: Principal<out Role, out SensitivePrincipalData>,
        sessionId: UUID,
        sessionInfo: SessionInfoRequest?,
        exchange: ServerWebExchange
    ): Result<RefreshToken, RefreshTokenCreationException> = coroutineBinding {
        val id = principal.id
            .mapError { ex -> RefreshTokenCreationException.InvalidPrincipal("Failed to generate refresh token because the associated principal document contains no ID: ${ex.message}", ex) }
            .bind()

        val refreshTokenId = Random.generateString(20)
            .mapError { ex ->
                RefreshTokenCreationException.Failed("Failed to create refresh token because no refresh token ID could be generated: ${ex.message}", ex)
            }
            .bind()

        updateSessions(exchange, principal, sessionId, sessionInfo, refreshTokenId)
            .mapError { ex ->
                RefreshTokenCreationException.Failed("Failed to create refresh token because updating session in the user document for user ${principal.id} failed: ${ex.message}", ex)
            }
            .andThen { create(id, sessionId, refreshTokenId) }
            .bind()
    }

    suspend fun create(
        principalId: ObjectId,
        sessionId: UUID,
        tokenId: String,
        issuedAt: Instant = Instant.now()
    ): Result<RefreshToken, RefreshTokenCreationException> = coroutineBinding {
        logger.debug { "Creating refresh token for user $principalId and session $sessionId" }

        val claims = runCatching {
            JwtClaimsSet.builder()
                .id(tokenId)
                .subject(principalId.toHexString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(jwtProperties.refreshExpiresIn))
                .claim(Constants.JWT_SESSION_CLAIM, sessionId)
                .build()
        }
            .mapError { ex -> RefreshTokenCreationException.Encoding("Failed to build claim set for refresh token: ${ex.message}", ex) }
            .bind()

        jwtService.encodeJwt(claims, tokenType.cookieName)
            .mapError { ex -> RefreshTokenCreationException.fromTokenCreationException(ex) }
            .map { jwt ->
                RefreshToken(principalId, sessionId, tokenId, jwt)
            }
            .bind()
    }

    private suspend fun updateSessions(
        exchange: ServerWebExchange,
        principal: Principal<out Role, out SensitivePrincipalData>,
        sessionId: UUID,
        sessionInfo: SessionInfoRequest?,
        tokenId: String
    ): Result<Principal<out Role, out SensitivePrincipalData>, RefreshTokenCreationException.SessionUpdate> = coroutineBinding {
        val ipAddress = exchange.request.getClientIp(geolocationProperties.realIpHeader)
            ?.let { ip -> 
                runCatching { InetAddress.getByName(ip) }
                    .onFailure { ex -> logger.warn { "Failed to resolve ip address from X-Real-Ip header: ${ex.message}" } }
                    .getOrElse { null } 
            }

        val location = ipAddress
            ?.let {
                geolocationService.getLocationResponse(ipAddress)
                    .onFailure { ex -> logger.warn { "Failed to resolve geolocation from ip address $ipAddress: ${ex.message}" } }
                    .getOrElse { null }
            }
            ?.let { location -> 
                SessionInfo.LocationInfo(
                    location.location.latitude,
                    location.location.longitude,
                    location.city.names["en"],
                    location.country.isoCode
                ) 
            }

        val sessionInfo = SessionInfo(
            refreshTokenId = tokenId,
            browser = sessionInfo?.browser,
            os = sessionInfo?.os,
            issuedAt = Instant.now(),
            ipAddress = ipAddress.toString(),
            location = location
        )

        principal.updateLastActive()
        principal.addOrUpdateSession(sessionId, sessionInfo)

        principalService.save(principal)
            .mapError { ex -> RefreshTokenCreationException.SessionUpdate("Failed to save user after updating sessions when creating refresh token: ${ex.message}", ex) }
            .bind()
    }

    /**
     * Extracts a [RefreshToken] from the provided JWT token value.
     *
     * This method decodes the given JWT token, validates its claims, and ensures it corresponds to an existing
     * and valid session for the associated user. If successful, a [RefreshToken] object is returned.
     * Otherwise, a [RefreshTokenExtractionException] describing the failure will be returned.
     *
     * @param tokenValue The JWT token value to be decoded and validated.
     * @return A [Result] containing either the successfully extracted [RefreshToken] or a [RefreshTokenExtractionException]
     */
    suspend fun extract(tokenValue: String): Result<RefreshToken, RefreshTokenExtractionException> {
        logger.debug { "Extracting refresh token" }

        return jwtService.decodeJwt(tokenValue, tokenType.cookieName)
            .mapError { ex -> RefreshTokenExtractionException.fromTokenExtractionException(ex) }
            .andThen { jwt ->
                coroutineBinding {
                    val userId = jwt.subject
                        .toResultOr { RefreshTokenExtractionException.Invalid("AccessToken does not contain sub") }
                        .andThen { sub ->
                            runCatching { ObjectId(sub) }
                                .mapError { ex -> RefreshTokenExtractionException.Invalid("Invalid ObjectId in sub: $sub", ex) }
                        }.bind()
    
                    val user = principalService.findById(userId)
                        .mapError { ex ->
                            RefreshTokenExtractionException.Invalid(
                                "Invalid refresh token: user with ID $userId does not exist",
                                ex
                            )
                        }
                        .bind()
    
                    val sessionId = (jwt.claims[Constants.JWT_SESSION_CLAIM] as? String)
                        .toResultOr { RefreshTokenExtractionException.Invalid("AccessToken does not contain session id") }
                        .andThen { s ->
                            runCatching { UUID.fromString(s) }.mapError { ex ->
                                RefreshTokenExtractionException.Invalid("Invalid session id: $s", ex)
                            }
                        }.bind()
    
                    val tokenId = jwt.id
                        .toResultOr { RefreshTokenExtractionException.Invalid("AccessToken does not contain token id") }
                        .bind()
    
                    val result = if (user.sensitive.sessions[sessionId]?.refreshTokenId != tokenId)
                        Err(RefreshTokenExtractionException.Invalid("Refresh token does not correspond to an existing session"))
                    else {
                        Ok(RefreshToken(userId, sessionId, tokenId, jwt))
                    }
    
                    result.bind()
                }
        }
    }

    /**
     * Extracts a [RefreshToken] from the provided [ServerWebExchange].
     *
     * This method retrieves a token value from the [ServerWebExchange], then decodes and validates it to extract a
     * [RefreshToken]. If successful, the [RefreshToken] is returned. Otherwise, a [RefreshTokenExtractionException]
     * describing the failure will be returned.
     *
     * @param exchange The [ServerWebExchange] containing the request context from which the token is to be extracted.
     * @return A [Result] containing either the successfully extracted [RefreshToken] or a [RefreshTokenExtractionException].
     */
    suspend fun extract(exchange: ServerWebExchange): Result<RefreshToken, RefreshTokenExtractionException> {
        return tokenValueExtractor.extractValue(exchange, tokenType, true)
            .mapError { ex -> RefreshTokenExtractionException.fromTokenExtractionException(ex) }
            .andThen { tokenValue ->
                extract(tokenValue)
            }
    }
}
