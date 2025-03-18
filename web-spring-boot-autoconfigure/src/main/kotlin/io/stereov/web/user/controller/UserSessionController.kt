package io.stereov.web.user.controller

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.auth.service.AuthenticationService
import io.stereov.web.user.dto.*
import io.stereov.web.user.service.CookieService
import io.stereov.web.user.service.UserSessionService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange

@RestController
@RequestMapping("/user")
class UserSessionController(
    private val authenticationService: AuthenticationService,
    private val userSessionService: UserSessionService,
    private val cookieService: CookieService,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    @GetMapping("/me")
    suspend fun getAccount(): ResponseEntity<UserDto> {
        val user = authenticationService.getCurrentUser()

        return ResponseEntity.ok(user.toDto())
    }

    @PostMapping("/login")
    suspend fun login(
        exchange: ServerWebExchange,
        @RequestBody payload: LoginRequest
    ): ResponseEntity<LoginResponse> {
        logger.info { "Executing login" }

        val user = userSessionService.checkCredentialsAndGetUser(payload)

        if (user.security.twoFactor.enabled) {
            val twoFactorCookie = cookieService.createTwoFactorSessionCookie(user.idX)
            return ResponseEntity.ok()
                .header("Set-Cookie", twoFactorCookie.toString())
                .body(LoginResponse(true, user.toDto()))
        }

        val ipAddress = exchange.request.remoteAddress?.address?.hostAddress

        val accessTokenCookie = cookieService.createAccessTokenCookie(user.idX, payload.device.id)
        val refreshTokenCookie = cookieService.createRefreshTokenCookie(user.idX, payload.device, ipAddress)

        return ResponseEntity.ok()
            .header("Set-Cookie", accessTokenCookie.toString())
            .header("Set-Cookie", refreshTokenCookie.toString())
            .body(LoginResponse(false, user.toDto()))
    }

    @PostMapping("/register")
    suspend fun register(
        exchange: ServerWebExchange,
        @RequestBody @Valid payload: RegisterUserDto
    ): ResponseEntity<UserDto> {
        logger.info { "Executing register" }

        val user = userSessionService.registerAndGetUser(payload)

        val ipAddress = exchange.request.remoteAddress?.address?.hostAddress

        val accessTokenCookie = cookieService.createAccessTokenCookie(user.idX, payload.device.id)
        val refreshTokenCookie = cookieService.createRefreshTokenCookie(user.idX, payload.device, ipAddress)

        return ResponseEntity.ok()
            .header("Set-Cookie", accessTokenCookie.toString())
            .header("Set-Cookie", refreshTokenCookie.toString())
            .body(user.toDto())
    }

    @PostMapping("/logout")
    suspend fun logout(@RequestBody deviceInfo: DeviceInfoRequestDto): ResponseEntity<Map<String, String>> {
        logger.info { "Executing logout" }

        val clearAccessTokenCookie = cookieService.clearAccessTokenCookie()
        val clearRefreshTokenCookie = cookieService.clearRefreshTokenCookie()

        userSessionService.logout(deviceInfo.id)

        return ResponseEntity.ok()
            .header("Set-Cookie", clearAccessTokenCookie.toString())
            .header("Set-Cookie", clearRefreshTokenCookie.toString())
            .body(mapOf("message" to "success"))
    }

    @PostMapping("/refresh")
    suspend fun refreshToken(
        exchange: ServerWebExchange,
        @RequestBody deviceInfoDto: DeviceInfoRequestDto
    ): ResponseEntity<UserDto> {
        logger.debug { "Refreshing token" }

        val ipAddress = exchange.request.remoteAddress?.address?.hostAddress

        val userDto = cookieService.validateRefreshTokenAndGetUserDto(exchange, deviceInfoDto.id)
        val userId = userDto.id

        val newAccessToken = cookieService.createAccessTokenCookie(userId, deviceInfoDto.id)
        val newRefreshToken = cookieService.createRefreshTokenCookie(userId, deviceInfoDto, ipAddress)

        return ResponseEntity.ok()
            .header("Set-Cookie", newAccessToken.toString())
            .header("Set-Cookie", newRefreshToken.toString())
            .body(userDto)
    }

    @DeleteMapping("/me")
    suspend fun delete(): ResponseEntity<Map<String, String>> {
        val clearAccessTokenCookie = cookieService.clearAccessTokenCookie()
        val clearRefreshTokenCookie = cookieService.clearRefreshTokenCookie()

        userSessionService.deleteUser()

        return ResponseEntity.ok()
            .header("Set-Cookie", clearAccessTokenCookie.toString())
            .header("Set-Cookie", clearRefreshTokenCookie.toString())
            .body(mapOf("message" to "success"))
    }
}
