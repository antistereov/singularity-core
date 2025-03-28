package io.stereov.web.user.service.token

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.config.Constants
import io.stereov.web.global.service.jwt.JwtService
import io.stereov.web.global.service.jwt.exception.model.InvalidTokenException
import io.stereov.web.global.service.jwt.model.AccessToken
import io.stereov.web.global.service.jwt.model.RefreshToken
import io.stereov.web.properties.JwtProperties
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class UserTokenService(
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    fun createAccessToken(userId: String, deviceId: String, expiration: Long = jwtProperties.expiresIn): String {
        logger.debug { "Creating access token for user $userId and device $deviceId" }

        val claims = JwtClaimsSet.builder()
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(expiration))
            .subject(userId)
            .claim(Constants.JWT_DEVICE_CLAIM, deviceId)
            .build()

        return jwtService.encodeJwt(claims)
    }

    suspend fun validateAndExtractAccessToken(token: String): AccessToken {
        logger.debug { "Validating access token" }

        val jwt = jwtService.decodeJwt(token, true)

        val accountId = jwt.subject
            ?: throw InvalidTokenException("JWT does not contain sub")

        val deviceId = jwt.claims[Constants.JWT_DEVICE_CLAIM] as? String
            ?: throw InvalidTokenException("JWT does not contain device id")

        return AccessToken(accountId, deviceId)
    }

    fun createRefreshToken(userId: String, deviceId: String): String {
        logger.debug { "Creating refresh token for user $userId and device $deviceId" }

        val claims = JwtClaimsSet.builder()
            .id(UUID.randomUUID().toString())
            .subject(userId)
            .claim("device_id", deviceId)
            .issuedAt(Instant.now())
            .build()

        return jwtService.encodeJwt(claims)
    }

    suspend fun extractRefreshToken(refreshToken: String, deviceId: String): RefreshToken {
        logger.debug { "Extracting refresh token" }

        val jwt = try {
            jwtService.decodeJwt(refreshToken, false)
        } catch (e: Exception) {
            throw InvalidTokenException("Cannot decode refresh token", e)
        }

        val accountId = jwt.subject
            ?: throw InvalidTokenException("Refresh token does not contain user id")

        return RefreshToken(accountId, deviceId, refreshToken)
    }
}
