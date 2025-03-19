package io.stereov.web.user.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.config.Constants
import io.stereov.web.global.service.geolocation.GeoLocationService
import io.stereov.web.global.service.jwt.JwtService
import io.stereov.web.global.service.jwt.exception.InvalidTokenException
import io.stereov.web.properties.AppProperties
import io.stereov.web.properties.JwtProperties
import io.stereov.web.user.dto.DeviceInfoRequest
import io.stereov.web.user.dto.UserDto
import io.stereov.web.user.model.DeviceInfo
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.time.Instant

@Service
class CookieService(
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties,
    private val appProperties: AppProperties,
    private val geoLocationService: GeoLocationService,
    private val userService: UserService,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    fun createAccessTokenCookie(userId: String, deviceId: String): ResponseCookie {
        logger.debug { "Creating access token cookie for user $userId" }

        val accessToken = jwtService.createAccessToken(userId, deviceId)

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

    suspend fun createRefreshTokenCookie(
        userId: String,
        deviceInfoDto: DeviceInfoRequest,
        ipAddress: String?
    ): ResponseCookie {
        logger.info { "Creating refresh token cookie" }

        val refreshToken = jwtService.createRefreshToken(userId, deviceInfoDto.id)

        val location = ipAddress?.let { geoLocationService.getLocation(it) }

        val deviceInfo = DeviceInfo(
            id = deviceInfoDto.id,
            tokenValue = refreshToken,
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

    suspend fun validateRefreshTokenAndGetUserDto(exchange: ServerWebExchange, deviceId: String): UserDto {
        logger.debug { "Validating refresh token and getting user" }

        val refreshTokenCookie = exchange.request.cookies[Constants.REFRESH_TOKEN_COOKIE]?.firstOrNull()?.value
            ?: throw InvalidTokenException("No refresh token provided")

        val refreshToken = jwtService.extractRefreshToken(refreshTokenCookie, deviceId)

        val user = userService.findById(refreshToken.accountId)

        if (user.devices.any { it.id == refreshToken.deviceId && it.tokenValue == refreshToken.value }) {
            return user.toDto()
        } else {
            throw InvalidTokenException("Invalid refresh token")
        }
    }

    suspend fun createTwoFactorSessionCookie(userId: String): ResponseCookie {
        logger.debug { "Creating cookie for two factor authentication token" }

        val cookie = ResponseCookie.from(Constants.TWO_FACTOR_AUTH_COOKIE, jwtService.createTwoFactorToken(userId))
            .httpOnly(true)
            .sameSite("Strict")
            .maxAge(0)
            .path("/")

        if (appProperties.secure) {
            cookie.secure(true)
        }

        return cookie.build()
    }

    suspend fun validateTwoFactorSessionCookieAndGetUserId(exchange: ServerWebExchange): String {
        logger.debug { "Validating two factor session cookie" }

        val twoFactorCookie = exchange.request.cookies[Constants.TWO_FACTOR_AUTH_COOKIE]?.firstOrNull()?.value
            ?: throw InvalidTokenException("No two factor authentication token provided")

        return jwtService.validateTwoFactorTokenAndExtractUserId(twoFactorCookie)
    }

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
}
