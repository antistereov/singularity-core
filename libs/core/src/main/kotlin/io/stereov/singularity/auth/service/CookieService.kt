package io.stereov.singularity.auth.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.exception.AuthException
import io.stereov.singularity.auth.exception.model.TwoFactorAuthDisabledException
import io.stereov.singularity.config.Constants
import io.stereov.singularity.global.service.geolocation.GeoLocationService
import io.stereov.singularity.global.service.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.global.service.random.RandomService
import io.stereov.singularity.global.service.twofactorauth.TwoFactorAuthService
import io.stereov.singularity.properties.AppProperties
import io.stereov.singularity.properties.JwtProperties
import io.stereov.singularity.user.dto.UserResponse
import io.stereov.singularity.user.dto.request.DeviceInfoRequest
import io.stereov.singularity.user.dto.request.TwoFactorStartSetupRequest
import io.stereov.singularity.user.model.DeviceInfo
import io.stereov.singularity.user.service.UserService
import io.stereov.singularity.user.service.token.TwoFactorAuthTokenService
import io.stereov.singularity.user.service.token.UserTokenService
import io.stereov.singularity.user.service.token.model.StepUpToken
import org.bson.types.ObjectId
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
    private val twoFactorAuthTokenService: TwoFactorAuthTokenService,
    private val authenticationService: AuthenticationService,
    private val twoFactorAuthService: TwoFactorAuthService,
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
    suspend fun createAccessTokenCookie(userId: ObjectId, deviceId: String): ResponseCookie {
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
     * @param exchange The server web exchange.
     *
     * @return The created refresh token cookie.
     */
    suspend fun createRefreshTokenCookie(
        userId: ObjectId,
        deviceInfoDto: DeviceInfoRequest,
        exchange: ServerWebExchange
    ): ResponseCookie {
        logger.info { "Creating refresh token cookie" }

        val refreshTokenId = RandomService.generateCode(20)
        val refreshToken = userTokenService.createRefreshToken(userId, deviceInfoDto.id, refreshTokenId)

        val ipAddress = exchange.request.remoteAddress?.address?.hostAddress
        val location = ipAddress?.let { try { geoLocationService.getLocation(it) } catch(_: Exception) { null } }

        val deviceInfo = DeviceInfo(
            id = deviceInfoDto.id,
            refreshTokenId = refreshTokenId,
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
    suspend fun validateRefreshTokenAndGetUserDto(exchange: ServerWebExchange, deviceId: String): UserResponse {
        logger.debug { "Validating refresh token and getting user" }

        val refreshTokenCookie = exchange.request.cookies[Constants.REFRESH_TOKEN_COOKIE]?.firstOrNull()?.value
            ?: throw InvalidTokenException("No refresh token provided")

        val refreshToken = userTokenService.extractRefreshToken(refreshTokenCookie, deviceId)

        val user = userService.findById(refreshToken.userId)

        if (user.sensitive.devices.any { it.id == refreshToken.deviceId && it.refreshTokenId == refreshToken.tokenId }) {
            return user.toResponse()
        } else {
            throw InvalidTokenException("Invalid refresh token")
        }
    }

    /**
     * Creates a cookie for the two-factor authentication for login verification.
     *
     * @param userId The ID of the user.
     *
     * @return The created two-factor authentication token cookie.
     */
    suspend fun createLoginVerificationCookie(userId: ObjectId): ResponseCookie {
        logger.debug { "Creating cookie for two factor login verification token" }

        val cookie = ResponseCookie.from(Constants.LOGIN_VERIFICATION_TOKEN_COOKIE, twoFactorAuthTokenService.createLoginToken(userId))
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
     * Validates the two-factor cookie for login verification and retrieves the user ID.
     *
     * @param exchange The server web exchange.
     *
     * @return The user ID associated with the login verification token.
     *
     * @throws InvalidTokenException If the login verification token is invalid or not provided.
     */
    suspend fun validateLoginVerificationCookieAndGetUserId(exchange: ServerWebExchange): ObjectId {
        logger.debug { "Validating login verification cookie" }

        val twoFactorCookie = exchange.request.cookies[Constants.LOGIN_VERIFICATION_TOKEN_COOKIE]?.firstOrNull()?.value
            ?: throw InvalidTokenException("No two factor authentication token provided")

        return twoFactorAuthTokenService.validateLoginTokenAndExtractUserId(twoFactorCookie)
    }

    /**
     * Clears the two-factor session cookie used for login verification.
     *
     * @return The cleared two-factor session cookie for login verification.
     */
    suspend fun clearLoginVerificationCookie(): ResponseCookie {
        logger.debug { "Clearing cookie for two factor authentication token" }

        val cookie = ResponseCookie.from(Constants.LOGIN_VERIFICATION_TOKEN_COOKIE, "")
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

        twoFactorAuthService.validateTwoFactorCode(user, code)
        val token = twoFactorAuthTokenService.createStepUpToken(code)

        return createStepUpCookie(token)
    }

    /**
     * Create a step-up token.
     * This function is only allowed to be called if the request path is /auth/2fa/recovery.
     * Therefore,
     * it can only be used if the user wants to recover his account after they lost access to their 2FA codes.
     *
     * @param userId The ID of the user.
     * @param deviceId The device ID of the device the user is trying to create a step-up token from.
     * @param exchange The server web exchange.
     *
     * @throws AuthException If this function is called from a path that is not /auth/2fa/recovery
     */
    internal suspend fun createStepUpCookieForRecovery(userId: ObjectId, deviceId: String, exchange: ServerWebExchange): ResponseCookie {
        logger.debug { "Creating step up cookie" }

        val token = twoFactorAuthTokenService.createStepUpTokenForRecovery(userId, deviceId, exchange)

        return createStepUpCookie(token)
    }

    private suspend fun createStepUpCookie(token: String): ResponseCookie {

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

        if (!authenticationService.getCurrentUser().sensitive.security.twoFactor.enabled) {
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

    suspend fun createTwoFactorSetupCookie(req: TwoFactorStartSetupRequest): ResponseCookie {
        val token = twoFactorAuthTokenService.createSetupStartToken(req)

        val cookie = ResponseCookie.from(Constants.TWO_FACTOR_SETUP_TOKEN_COOKIE, token)
            .httpOnly(true)
            .sameSite("Strict")
            .maxAge(jwtProperties.expiresIn)
            .path("/")
            .secure(appProperties.secure)
            .build()

        return cookie
    }

    suspend fun validateTwoFactorSetupCookie(exchange: ServerWebExchange) {

        val token = exchange.request.cookies[Constants.TWO_FACTOR_SETUP_TOKEN_COOKIE]?.firstOrNull()?.value
            ?: throw InvalidTokenException("No setup token provided")

        twoFactorAuthTokenService.validateSetupStartToken(token)
    }
}
