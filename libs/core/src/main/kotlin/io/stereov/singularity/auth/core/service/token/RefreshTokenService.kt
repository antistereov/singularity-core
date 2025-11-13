package io.stereov.singularity.auth.core.service.token

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.component.TokenValueExtractor
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.exception.AccessTokenCreationException
import io.stereov.singularity.auth.core.model.SessionInfo
import io.stereov.singularity.auth.core.model.token.RefreshToken
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.auth.geolocation.properties.GeolocationProperties
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.jwt.exception.TokenCreationException
import io.stereov.singularity.auth.jwt.exception.TokenExtractionException
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.global.util.Constants
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.global.util.catchAs
import io.stereov.singularity.global.util.getClientIp
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.time.Instant
import java.util.*

/**
 * Create and extract [RefreshToken]s.
 *
 * @author antistereov
 */
@Service
class RefreshTokenService(
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties,
    private val geolocationService: GeolocationService,
    private val geolocationProperties: GeolocationProperties,
    private val userService: UserService,
    private val tokenValueExtractor: TokenValueExtractor
) {

    private val logger = KotlinLogging.logger {}
    private val tokenType = SessionTokenType.Refresh

    /**
     * Create a [RefreshToken] for a given user and session.
     * It will automatically update the session information for the user when creating a new [RefreshToken].
     *
     * @param user The [UserDocument] to create the token for.
     * @param sessionId The session ID this refresh token will belong to.
     * @param sessionInfo Optional information about the session.
     * @param exchange The [ServerWebExchange] where the client IP address will be read from.
     *
     * @return A new [RefreshToken].
     */
    suspend fun create(
        user: UserDocument,
        sessionId: UUID,
        sessionInfo: SessionInfoRequest?,
        exchange: ServerWebExchange
    ): Result<RefreshToken, AccessTokenCreationException> {
        val refreshTokenId = Random.generateString(20)

        return updateSessions(exchange, user, sessionId, sessionInfo, refreshTokenId)
            .mapError { ex ->
                AccessTokenCreationException.Failed("Failed to create refresh token because updating session in the user document for user ${user.id} failed: ${ex.message}", ex)
            }
            .andThen {
                doCreate(user.id, sessionId, refreshTokenId)
                    .mapError { ex ->
                        AccessTokenCreationException.Encoding("Failed to encode refresh token: ${ex.message}", ex)
                    }
            }

    }

    private suspend fun doCreate(
        userId: ObjectId,
        sessionId: UUID,
        tokenId: String,
        issuedAt: Instant = Instant.now()
    ): Result<RefreshToken, TokenCreationException.Encoding> {
        logger.debug { "Creating refresh token for user $userId and session $sessionId" }

        val claims = JwtClaimsSet.builder()
            .id(tokenId)
            .subject(userId.toHexString())
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(jwtProperties.refreshExpiresIn))
            .claim(Constants.JWT_SESSION_CLAIM, sessionId)
            .build()

        return jwtService.encodeJwt(claims, tokenType.cookieName).map { jwt ->
            RefreshToken(userId, sessionId, tokenId, jwt)
        }
    }

    private suspend fun updateSessions(
        exchange: ServerWebExchange,
        user: UserDocument,
        sessionId: UUID,
        sessionInfo: SessionInfoRequest?,
        tokenId: String
    ): Result<UserDocument, Throwable> = coroutineBinding {
        val ipAddress = runCatching {
            exchange.request.getClientIp(geolocationProperties.realIpHeader)
        }.bind()
        val location = runCatching {
            geolocationService.getLocation(exchange.request)
        }.bind()

        val sessionInfo = SessionInfo(
            refreshTokenId = tokenId,
            browser = sessionInfo?.browser,
            os = sessionInfo?.os,
            issuedAt = Instant.now(),
            ipAddress = ipAddress,
            location = SessionInfo.LocationInfo(
                location.location.latitude,
                location.location.longitude,
                location.city.names["en"],
                location.country.isoCode
            )
        )

        user.updateLastActive()
        user.addOrUpdatesession(sessionId, sessionInfo)

        runCatching { userService.save(user) }.bind()
    }

    /**
     * Extract a [RefreshToken] from a given value.
     *
     * @param tokenValue The token value as [String].
     *
     * @return The [RefreshToken]
     */
    suspend fun extract(tokenValue: String): Result<RefreshToken, TokenExtractionException> {
        logger.debug { "Extracting refresh token" }

        return jwtService.decodeJwt(tokenValue, tokenType.cookieName).andThen { jwt ->
            coroutineBinding {
                val userId = jwt.subject
                    .toResultOr { TokenExtractionException.Invalid("AccessToken does not contain sub") }
                    .andThen { sub ->
                        runCatching { ObjectId(sub) }
                            .mapError { ex -> TokenExtractionException.Invalid("Invalid ObjectId in sub: $sub", ex) }
                    }.bind()

                val user = runCatching { userService.findById(userId) }
                    .mapError { ex ->
                        TokenExtractionException.Invalid(
                            "Invalid refresh token: user with ID $userId does not exist",
                            ex
                        )
                    }
                    .bind()

                val sessionId = (jwt.claims[Constants.JWT_SESSION_CLAIM] as? String)
                    .toResultOr { TokenExtractionException.Invalid("AccessToken does not contain session id") }
                    .andThen { s ->
                        catchAs({ UUID.fromString(s) }) { ex ->
                            TokenExtractionException.Invalid("Invalid session id: $s", ex)
                        }
                    }.bind()

                val tokenId = jwt.id
                    .toResultOr { TokenExtractionException.Invalid("AccessToken does not contain token id") }
                    .bind()

                val result = if (user.sensitive.sessions[sessionId]?.refreshTokenId != tokenId)
                    Err(TokenExtractionException.Invalid("Refresh token does not correspond to an existing session"))
                else {
                    Ok(RefreshToken(userId, sessionId, tokenId, jwt))
                }

                result.bind()
            }
        }
    }

    /**
     * Extracts a [RefreshToken] from a [ServerWebExchange].
     *
     * @param exchange The [ServerWebExchange].
     *
     * @return The [RefreshToken].
     */
    suspend fun extract(exchange: ServerWebExchange): Result<RefreshToken, TokenExtractionException> {
        return tokenValueExtractor.extractValue(exchange, tokenType, true)
            .andThen { tokenValue ->
                extract(tokenValue)
            }
    }
}
