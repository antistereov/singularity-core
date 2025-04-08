package io.stereov.web.auth.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.auth.exception.model.TwoFactorAuthDisabledException
import io.stereov.web.config.Constants
import io.stereov.web.global.service.geolocation.GeoLocationService
import io.stereov.web.global.service.jwt.exception.model.InvalidTokenException
import io.stereov.web.global.service.twofactorauth.TwoFactorAuthService
import io.stereov.web.properties.AppProperties
import io.stereov.web.properties.JwtProperties
import io.stereov.web.user.dto.UserDto
import io.stereov.web.user.dto.request.DeviceInfoRequest
import io.stereov.web.user.model.DeviceInfo
import io.stereov.web.user.service.UserService
import io.stereov.web.user.service.token.TwoFactorAuthTokenService
import io.stereov.web.user.service.token.UserTokenService
import io.stereov.web.user.service.token.model.StepUpToken
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.time.Instant

/**
 * # Service for managing cookies related to user authentication.
 *
 * This service provides methods to create, clear, and validate cookies for access tokens,
 * refresh tokens, and two-factor authentication.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Service
class CookieService(
    private val userTokenService: UserTokenService,
    private val jwtProperties: JwtProperties,
    private val appProperties: AppProperties,
    private val geoLocationService: GeoLocationService,
    private val userService: UserService,
    private val twoFactorAuthService: TwoFactorAuthService,
    private val twoFactorAuthTokenService: TwoFactorAuthTokenService,
    private val authenticationService: AuthenticationService,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Creates a cookie for the access token.
     *
     * @param userId The ID of the user.
     * @param deviceId The ID of the device.
     *
     * @return The created access token cookie.
     */
    suspend fun createAccessTokenCookie(userId: String, deviceId: String): ResponseCookie {
        logger.debug { "Creating access token cookie for user $userId" }

        val accessToken = userTokenService.createAccessToken(userId, deviceId)

        val cookie = ResponseCookie.from(Constants.ACCESS_TOKEN_COOKIE, accessToken)
            .httpOnly(true)
            .sameSite("Strict")
            .maxAge(jwtProperties.expiresIn)
            .path("/")

        if (appProperties.secure) {
            cookie.secure(true)
        }
        return cookie.build()
    }

    /**
     * Creates a cookie for the refresh token.
     *
     * @param userId The ID of the user.
     * @param deviceInfoDto The device information.
     * @param ipAddress The IP address of the user.
     *
     * @return The created refresh token cookie.
     */
    suspend fun createRefreshTokenCookie(
        userId: String,
        deviceInfoDto: DeviceInfoRequest,
        ipAddress: String?
    ): ResponseCookie {
        logger.info { "Creating refresh token cookie" }

        val refreshToken = userTokenService.createRefreshToken(userId, deviceInfoDto.id)

        val location = ipAddress?.let { try { geoLocationService.getLocation(it) } catch(e: Exception) { null } }

        val deviceInfo = DeviceInfo(
            id = deviceInfoDto.id,
            refreshTokenValue = refreshToken,
            browser = deviceInfoDto.browser,
            os = deviceInfoDto.os,
            issuedAt = Instant.now(),
            ipAddress = ipAddress,
            location = if (location != null) {
                DeviceInfo.LocationInfo(
                    location.latitude,
                    location.longitude,
                    location.cityName,
                    location.regionName,
                    location.countryCode
                )
            } else null,
        )

        val user = userService.findById(userId)
        user.addOrUpdateDevice(deviceInfo)
        userService.save(user)

        val cookie = ResponseCookie.from(Constants.REFRESH_TOKEN_COOKIE, refreshToken)
            .httpOnly(true)
            .sameSite("Strict")
            .path("/")
            .maxAge(10L * 365 * 24 * 60 * 60)

        if (appProperties.secure) {
            cookie.secure(true)
        }

        return cookie.build()
    }

    /**
     * Clears the access token cookie.
     *
     * @return The cleared access token cookie.
     */
    fun clearAccessTokenCookie(): ResponseCookie {
        logger.debug { "Clearing access token cookie" }

        val cookie = ResponseCookie.from(Constants.ACCESS_TOKEN_COOKIE, "")
            .httpOnly(true)
            .sameSite("Strict")
            .maxAge(0)
            .path("/")

        if (appProperties.secure) {
            cookie.secure(true)
        }

        return cookie.build()

    }

    /**
     * Clears the refresh token cookie.
     *
     * @return The cleared refresh token cookie.
     */
    fun clearRefreshTokenCookie(): ResponseCookie {
        logger.debug { "Clearing refresh token cookie" }

        val cookie = ResponseCookie.from(Constants.REFRESH_TOKEN_COOKIE, "")
            .httpOnly(true)
            .sameSite("Strict")
            .maxAge(0)
            .path("/")

        if (appProperties.secure) {
            cookie.secure(true)
        }

        return cookie.build()
    }

    /**
     * Validates the refresh token and retrieves the user information.
     *
     * @param exchange The server web exchange.
     * @param deviceId The ID of the device.
     *
     * @return The user information.
     */
    suspend fun validateRefreshTokenAndGetUserDto(exchange: ServerWebExchange, deviceId: String): UserDto {
        logger.debug { "Validating refresh token and getting user" }

        val refreshTokenCookie = exchange.request.cookies[Constants.REFRESH_TOKEN_COOKIE]?.firstOrNull()?.value
            ?: throw InvalidTokenException("No refresh token provided")

        val refreshToken = userTokenService.extractRefreshToken(refreshTokenCookie, deviceId)

        val user = userService.findById(refreshToken.accountId)

        if (user.devices.any { it.id == refreshToken.deviceId && it.refreshTokenValue == refreshToken.value }) {
            return user.toDto()
        } else {
            throw InvalidTokenException("Invalid refresh token")
        }
    }

    /**
     * Creates a cookie for the two-factor authentication token.
     *
     * @param userId The ID of the user.
     *
     * @return The created two-factor authentication token cookie.
     */
    suspend fun createTwoFactorSessionCookie(userId: String): ResponseCookie {
        logger.debug { "Creating cookie for two factor authentication token" }

        val cookie = ResponseCookie.from(Constants.TWO_FACTOR_AUTH_COOKIE, twoFactorAuthTokenService.createTwoFactorToken(userId))
            .httpOnly(true)
            .sameSite("Strict")
            .maxAge(jwtProperties.expiresIn)
            .path("/")

        if (appProperties.secure) {
            cookie.secure(true)
        }

        return cookie.build()
    }

    /**
     * Validates the two-factor authentication session cookie and retrieves the user ID.
     *
     * @param exchange The server web exchange.
     *
     * @return The user ID associated with the two-factor authentication session.
     *
     * @throws InvalidTokenException If the two-factor authentication token is invalid or not provided.
     */
    suspend fun validateTwoFactorSessionCookieAndGetUserId(exchange: ServerWebExchange): String {
        logger.debug { "Validating two factor session cookie" }

        val twoFactorCookie = exchange.request.cookies[Constants.TWO_FACTOR_AUTH_COOKIE]?.firstOrNull()?.value
            ?: throw InvalidTokenException("No two factor authentication token provided")

        return twoFactorAuthTokenService.validateTwoFactorTokenAndExtractUserId(twoFactorCookie)
    }

    /**
     * Clears the two-factor authentication session cookie.
     *
     * @return The cleared two-factor authentication session cookie.
     */
    suspend fun clearTwoFactorSessionCookie(): ResponseCookie {
        logger.debug { "Clearing cookie for two factor authentication token" }

        val cookie = ResponseCookie.from(Constants.TWO_FACTOR_AUTH_COOKIE, "")
            .httpOnly(true)
            .sameSite("Strict")
            .maxAge(0)
            .path("/")

        if (appProperties.secure) {
            cookie.secure(true)
        }

        return cookie.build()
    }

    /**
     * Creates a step-up cookie for two-factor authentication.
     *
     * @param code The two-factor authentication code.
     *
     * @return The created step-up cookie.
     */
    suspend fun createStepUpCookie(code: Int): ResponseCookie {
        logger.debug { "Creating step up cookie" }

        val user = authenticationService.getCurrentUser()

        if (!user.security.twoFactor.enabled) {
            throw TwoFactorAuthDisabledException()
        }

        twoFactorAuthService.validateTwoFactorCode(user, code)

        val token = twoFactorAuthTokenService.createStepUpToken(code)

        val cookie = ResponseCookie.from(Constants.STEP_UP_TOKEN_COOKIE, token)
            .httpOnly(true)
            .sameSite("Strict")
            .maxAge(jwtProperties.expiresIn)
            .path("/")
            .secure(appProperties.secure)
            .build()

        return cookie
    }

    /**
     * Validates the step-up cookie and retrieves the token values.
     *
     * @param exchange The server web exchange.
     *
     * @return The step-up token.
     *
     * @throws InvalidTokenException If the step-up token is invalid or not provided.
     */
    suspend fun validateStepUpCookie(exchange: ServerWebExchange): StepUpToken {
        logger.debug { "Validating step up token" }

        if (!authenticationService.getCurrentUser().security.twoFactor.enabled) {
            throw TwoFactorAuthDisabledException()
        }

        val stepUpCookie = exchange.request.cookies[Constants.STEP_UP_TOKEN_COOKIE]?.firstOrNull()?.value
            ?: throw InvalidTokenException("No step up token provided")

        return twoFactorAuthTokenService.validateAndExtractStepUpToken(stepUpCookie)
    }

    /**
     * Clears the step-up cookie.
     *
     * @return The cleared step-up cookie.
     */
    suspend fun clearStepUpCookie(): ResponseCookie {
        logger.debug { "Clearing cookie for step up token" }

        val cookie = ResponseCookie.from(Constants.STEP_UP_TOKEN_COOKIE, "")
            .httpOnly(true)
            .sameSite("Strict")
            .maxAge(0)
            .path("/")
            .secure(appProperties.secure)
            .build()

        return cookie
    }
}
