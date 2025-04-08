package io.stereov.web.user.service.token

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.config.Constants
import io.stereov.web.global.service.cache.AccessTokenCache
import io.stereov.web.global.service.jwt.JwtService
import io.stereov.web.global.service.jwt.exception.model.InvalidTokenException
import io.stereov.web.properties.JwtProperties
import io.stereov.web.user.service.token.model.AccessToken
import io.stereov.web.user.service.token.model.RefreshToken
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

/**
 * # Service for managing user tokens.
 *
 * This service provides methods to create, validate, and extract access and refresh tokens.
 * It uses the [JwtService] to handle JWT encoding and decoding.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Service
class UserTokenService(
    private val jwtService: JwtService,
    private val accessTokenCache: AccessTokenCache,
    jwtProperties: JwtProperties,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    private val expiresIn = jwtProperties.expiresIn

    /**
     * Creates an access token for the given user ID and device ID.
     *
     * @param userId The ID of the user.
     * @param deviceId The ID of the device.
     * @param issuedAt The time the token is issued. Default is the current time.
     *
     * @return The generated access token.
     */
    suspend fun createAccessToken(userId: String, deviceId: String, issuedAt: Instant = Instant.now()): String {
        logger.debug { "Creating access token for user $userId and device $deviceId" }

        val tokenId = UUID.randomUUID().toString()

        accessTokenCache.addTokenId(userId, tokenId)

        val claims = JwtClaimsSet.builder()
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(expiresIn))
            .subject(userId)
            .claim(Constants.JWT_DEVICE_CLAIM, deviceId)
            .id(tokenId)
            .build()

        return jwtService.encodeJwt(claims)
    }

    /**
     * Validates the given access token and extracts the user ID, device ID, and token ID.
     *
     * @param token The access token to validate.
     *
     * @return An [AccessToken] object containing the user ID, device ID, and token ID.
     *
     * @throws InvalidTokenException If the token is invalid or does not contain the required claims.
     */
    suspend fun validateAndExtractAccessToken(token: String): AccessToken {
        logger.debug { "Validating access token" }

        val jwt = jwtService.decodeJwt(token, true)

        val userId = jwt.subject
            ?: throw InvalidTokenException("JWT does not contain sub")

        val deviceId = jwt.claims[Constants.JWT_DEVICE_CLAIM] as? String
            ?: throw InvalidTokenException("JWT does not contain device id")

        val tokenId = jwt.id
            ?: throw InvalidTokenException("JWT does not contain token id")

        val isValid = accessTokenCache.isTokenIdValid(userId, tokenId)

        if (!isValid) {
            throw InvalidTokenException("Access token is not valid")
        }

        return AccessToken(userId, deviceId, tokenId)
    }

    /**
     * Creates a refresh token for the given user ID and device ID.
     *
     * @param userId The ID of the user.
     * @param deviceId The ID of the device.
     *
     * @return The generated refresh token.
     */
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

    /**
     * Validates the given refresh token and extracts the user ID and device ID.
     *
     * @param refreshToken The refresh token to validate.
     * @param deviceId The ID of the device.
     *
     * @return A [RefreshToken] object containing the user ID, device ID, and token value.
     *
     * @throws InvalidTokenException If the token is invalid or does not contain the required claims.
     */
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
