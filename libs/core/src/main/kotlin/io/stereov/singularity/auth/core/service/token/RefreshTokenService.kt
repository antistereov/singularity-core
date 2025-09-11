package io.stereov.singularity.auth.core.service.token

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.exception.AuthException
import io.stereov.singularity.auth.core.model.token.RefreshToken
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.auth.core.component.TokenValueExtractor
import io.stereov.singularity.auth.geolocation.properties.GeolocationProperties
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.global.util.Constants
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.global.util.getClientIp
import io.stereov.singularity.user.core.model.SessionInfo
import io.stereov.singularity.user.core.service.UserService
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.time.Instant

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

    suspend fun create(userId: ObjectId, sessionId: String,
                       tokenId: String, issuedAt: Instant = Instant.now()): RefreshToken {
        logger.debug { "Creating refresh token for user $userId and session $sessionId" }

        val claims = JwtClaimsSet.builder()
            .id(tokenId)
            .subject(userId.toHexString())
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(jwtProperties.refreshExpiresIn))
            .claim(Constants.JWT_session_CLAIM, sessionId)
            .build()

        val jwt = jwtService.encodeJwt(claims)

        return RefreshToken(userId, sessionId, tokenId, jwt)
    }

    private suspend fun updatesessions(exchange: ServerWebExchange,
                                       userId: ObjectId, sessionInfo: SessionInfoRequest, tokenId: String) {
        val ipAddress = exchange.request.getClientIp(geolocationProperties.realIpHeader)
        val location = geolocationService.getLocationOrNull(exchange.request)

        val sessionInfo = SessionInfo(
            id = sessionInfo.id,
            refreshTokenId = tokenId,
            browser = sessionInfo.browser,
            os = sessionInfo.os,
            issuedAt = Instant.now(),
            ipAddress = ipAddress,
            location = location?.let {
                SessionInfo.LocationInfo(
                    location.location.latitude,
                    location.location.longitude,
                    location.city.names["en"],
                    location.country.isoCode
                )
            },
        )

        val user = userService.findById(userId)
        user.addOrUpdatesession(sessionInfo)
        userService.save(user)
    }

    suspend fun create(
        userId: ObjectId,
        sessionInfo: SessionInfoRequest,
        exchange: ServerWebExchange
    ): RefreshToken {
        val refreshTokenId = Random.Companion.generateString(20)
        val refreshToken = create(userId, sessionInfo.id, refreshTokenId)

        updatesessions(exchange, userId, sessionInfo, refreshTokenId)

        return refreshToken
    }

    suspend fun extract(exchange: ServerWebExchange, sessionId: String): RefreshToken {
        logger.debug { "Extracting refresh token" }

        val tokenValue = tokenValueExtractor.extractValue(exchange, tokenType, true)

        val jwt = try {
            jwtService.decodeJwt(tokenValue, true)
        } catch (e: Exception) {
            throw InvalidTokenException("Cannot decode refresh token", e)
        }

        val userId = jwt.subject?.let { ObjectId(it) }
            ?: throw InvalidTokenException("Refresh token does not contain user id")

        val tokenId = jwt.id
            ?: throw InvalidTokenException("Refresh token does not contain token id")

        val user = userService.findByIdOrNull(userId)
            ?: throw AuthException("Invalid access token: user does not exist")

        // Check if the refresh token is linked to a session
        if (user.sensitive.sessions.none { it.id == sessionId && it.refreshTokenId == tokenId }) {
            throw InvalidTokenException("Refresh token does not correspond to an ")
        }

        return RefreshToken(userId, sessionId, tokenId, jwt)
    }
}