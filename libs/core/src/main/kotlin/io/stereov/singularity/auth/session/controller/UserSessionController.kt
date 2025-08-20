package io.stereov.singularity.auth.session.controller

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.AuthenticationService
import io.stereov.singularity.auth.core.service.CookieService
import io.stereov.singularity.auth.device.dto.DeviceInfoRequest
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.session.dto.request.LoginRequest
import io.stereov.singularity.auth.session.dto.request.RegisterUserRequest
import io.stereov.singularity.auth.session.dto.response.LoginResponse
import io.stereov.singularity.auth.session.dto.response.RegisterResponse
import io.stereov.singularity.auth.session.service.UserSessionService
import io.stereov.singularity.content.translate.model.Language
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.core.mapper.UserMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange

/**
 * # UserSessionController
 *
 * This controller handles user session-related operations such as login, registration, and user information retrieval.
 *
 * It provides endpoints for:
 * - Retrieving the current user's information
 * - Logging in a user
 * - Registering a new user
 * - Changing user email, password, and other information
 * - Logging out a user
 * - Refreshing user tokens
 * - Deleting a user account
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "User Session", description = "Operations related to login and session management")
class UserSessionController(
    private val authenticationService: AuthenticationService,
    private val userSessionService: UserSessionService,
    private val cookieService: CookieService,
    private val userMapper: UserMapper,
    private val authProperties: AuthProperties,
    private val geoLocationService: GeolocationService
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Get the current user's information.
     *
     * @return The current user's information as a [UserResponse].
     */
    @GetMapping("/me")
    @Operation(
        summary = "Get currently logged in user",
    )
    @SecurityRequirement(name = "bearerAuth")
    suspend fun getUser(): ResponseEntity<UserResponse> {
        val user = authenticationService.getCurrentUser()

        return ResponseEntity.ok(userMapper.toResponse(user))
    }

    /**
     * Get the current user's information with application info.
     *
     * @return The current user's information with application info as a [UserResponse].
     */
    @PostMapping("/login")
    suspend fun login(
        exchange: ServerWebExchange,
        @RequestBody payload: LoginRequest
    ): ResponseEntity<LoginResponse> {
        logger.info { "Executing login" }

        val user = userSessionService.checkCredentialsAndGetUser(payload)

        if (user.sensitive.security.twoFactor.enabled) {
            val twoFactorCookie = cookieService.createLoginVerificationCookie(user.id)
            return ResponseEntity.ok()
                .header("Set-Cookie", twoFactorCookie.toString())
                .body(LoginResponse(true, userMapper.toResponse(user)))
        }

        val accessTokenCookie = cookieService.createAccessTokenCookie(user.id, payload.device.id)
        val refreshTokenCookie = cookieService.createRefreshTokenCookie(user.id, payload.device, exchange)

        val res = LoginResponse(
            false,
            userMapper.toResponse(user),
            if (authProperties.allowHeaderAuthentication) accessTokenCookie.value else null,
            if (authProperties.allowHeaderAuthentication) refreshTokenCookie.value else null,
            geoLocationService.getLocationOrNull(exchange.request)
        )

        return ResponseEntity.ok()
            .header("Set-Cookie", accessTokenCookie.toString())
            .header("Set-Cookie", refreshTokenCookie.toString())
            .body(res)
    }

    /**
     * Register a new user.
     *
     * @param exchange The server web exchange.
     * @param payload The registration request payload.
     * @return The registered user's information as a [UserResponse].
     */
    @PostMapping("/register")
    suspend fun register(
        exchange: ServerWebExchange,
        @RequestBody @Valid payload: RegisterUserRequest,
        @RequestParam("send-email") sendEmail: Boolean = true,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<RegisterResponse> {
        logger.info { "Executing register" }

        val user = userSessionService.registerAndGetUser(payload, sendEmail, lang)

        val accessTokenCookie = cookieService.createAccessTokenCookie(user.id, payload.device.id)
        val refreshTokenCookie = cookieService.createRefreshTokenCookie(user.id, payload.device, exchange)

        val res = RegisterResponse(
            userMapper.toResponse(user),
            if (authProperties.allowHeaderAuthentication) accessTokenCookie.value else null,
            if (authProperties.allowHeaderAuthentication) refreshTokenCookie.value else null,
            geoLocationService.getLocationOrNull(exchange.request)
        )

        return ResponseEntity.ok()
            .header("Set-Cookie", accessTokenCookie.toString())
            .header("Set-Cookie", refreshTokenCookie.toString())
            .body(res)
    }

    /**
     * Set up two-factor authentication for the user.
     *
     * @param deviceInfo The device info request payload.
     * @return The user's information as a [UserResponse].
     */
    @PostMapping("/logout")
    suspend fun logout(@RequestBody deviceInfo: DeviceInfoRequest): ResponseEntity<Map<String, String>> {
        logger.info { "Executing logout" }

        val clearAccessTokenCookie = cookieService.clearAccessTokenCookie()
        val clearRefreshTokenCookie = cookieService.clearRefreshTokenCookie()
        val clearStepUpTokenCookie = cookieService.clearStepUpCookie()

        userSessionService.logout(deviceInfo.id)

        return ResponseEntity.ok()
            .header("Set-Cookie", clearAccessTokenCookie.toString())
            .header("Set-Cookie", clearRefreshTokenCookie.toString())
            .header("Set-Cookie", clearStepUpTokenCookie.toString())
            .body(mapOf("message" to "success"))
    }

    /**
     * Logs out the user from all devices.
     *
     * @return A response indicating the success of the operation.
     */
    @PostMapping("/logout-all")
    suspend fun logoutFromAllDevices(): ResponseEntity<Map<String, String>> {
        logger.debug { "Logging out user from all devices" }

        val clearAccessTokenCookie = cookieService.clearAccessTokenCookie()
        val clearRefreshTokenCookie = cookieService.clearRefreshTokenCookie()
        val clearStepUpTokenCookie = cookieService.clearStepUpCookie()

        userSessionService.logoutAllDevices()

        return ResponseEntity.ok()
            .header("Set-Cookie", clearAccessTokenCookie.toString())
            .header("Set-Cookie", clearRefreshTokenCookie.toString())
            .header("Set-Cookie", clearStepUpTokenCookie.toString())
            .body(mapOf("message" to "success"))
    }

    /**
     * Refresh the user's access token.
     *
     * @param exchange The server web exchange.
     * @param deviceInfoDto The device information request payload.
     * @return The user's information as a [UserResponse].
     */
    @PostMapping("/refresh")
    suspend fun refreshToken(
        exchange: ServerWebExchange,
        @RequestBody deviceInfoDto: DeviceInfoRequest
    ): ResponseEntity<UserResponse> {
        logger.debug { "Refreshing token" }

        val userDto = cookieService.validateRefreshTokenAndGetUserDto(exchange, deviceInfoDto.id)
        val userId = userDto.id

        val newAccessToken = cookieService.createAccessTokenCookie(userId, deviceInfoDto.id)
        val newRefreshToken = cookieService.createRefreshTokenCookie(userId, deviceInfoDto, exchange)

        return ResponseEntity.ok()
            .header("Set-Cookie", newAccessToken.toString())
            .header("Set-Cookie", newRefreshToken.toString())
            .body(userDto)
    }
}
