package io.stereov.web.user.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.properties.BackendProperties
import io.stereov.web.properties.JwtProperties
import io.stereov.web.user.dto.DeviceInfoRequestDto
import io.stereov.web.user.model.DeviceInfo
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import io.stereov.web.config.Constants
import io.stereov.web.global.service.geolocation.GeoLocationService
import io.stereov.web.global.service.jwt.JwtService
import io.stereov.web.global.service.jwt.exception.InvalidTokenException
import io.stereov.web.user.dto.UserDto
import org.springframework.web.server.ServerWebExchange

@Service
class CookieService(
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties,
    private val backendProperties: BackendProperties,
    private val geoLocationService: GeoLocationService,
    private val userService: UserService,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    fun createAccessTokenCookie(accountId: String): ResponseCookie {
        logger.debug { "Creating access token cookie for account $accountId" }

        val accessToken = jwtService.createAccessToken(accountId)

        val cookie = ResponseCookie.from(Constants.ACCESS_TOKEN_COOKIE, accessToken)
            .httpOnly(true)
            .sameSite("Strict")
            .maxAge(jwtProperties.expiresIn)
            .path("/")

        if (backendProperties.secure) {
            cookie.secure(true)
        }
        return cookie.build()
    }

    suspend fun createRefreshTokenCookie(
        userId: String,
        deviceInfoDto: DeviceInfoRequestDto,
        ipAddress: String?
    ): ResponseCookie {
        val refreshToken = jwtService.createRefreshToken(userId, deviceInfoDto.id)

        val location = ipAddress?.let { geoLocationService.getLocation(it) }

        val deviceInfo = DeviceInfo(
            id = deviceInfoDto.id,
            tokenValue = refreshToken,
            browser = deviceInfoDto.browser,
            os = deviceInfoDto.os,
            issuedAt = System.currentTimeMillis(),
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

        userService.addOrUpdateDevice(userId, deviceInfo)

        val cookie = ResponseCookie.from(Constants.REFRESH_TOKEN_COOKIE, refreshToken)
            .httpOnly(true)
            .sameSite("Strict")
            .path("/")
            .maxAge(10L * 365 * 24 * 60 * 60)

        if (backendProperties.secure) {
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

        if (backendProperties.secure) {
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

        if (backendProperties.secure) {
            cookie.secure(true)
        }

        return cookie.build()
    }

    suspend fun validateRefreshTokenAndGetUserDto(exchange: ServerWebExchange, deviceId: String): UserDto {
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
}
