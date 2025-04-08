package io.stereov.web.user.controller

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.auth.service.AuthenticationService
import io.stereov.web.auth.service.CookieService
import io.stereov.web.user.dto.ApplicationInfoDto
import io.stereov.web.user.dto.UserDto
import io.stereov.web.user.dto.request.*
import io.stereov.web.user.dto.response.LoginResponse
import io.stereov.web.user.service.UserSessionService
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
@RequestMapping("/user")
class UserSessionController(
    private val authenticationService: AuthenticationService,
    private val userSessionService: UserSessionService,
    private val cookieService: CookieService,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Get the current user's information.
     *
     * @return The current user's information as a [UserDto].
     */
    @GetMapping("/me")
    suspend fun getUser(): ResponseEntity<UserDto> {
        val user = authenticationService.getCurrentUser()

        return ResponseEntity.ok(user.toDto())
    }

    /**
     * Get the current user's information with application info.
     *
     * @return The current user's information with application info as a [UserDto].
     */
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

    /**
     * Register a new user.
     *
     * @param exchange The server web exchange.
     * @param payload The registration request payload.
     * @return The registered user's information as a [UserDto].
     */
    @PostMapping("/register")
    suspend fun register(
        exchange: ServerWebExchange,
        @RequestBody @Valid payload: RegisterUserRequest
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

    /**
     * Verify the two-factor authentication code.
     *
     * @param payload The two-factor authentication request payload.
     * @param exchange The server web exchange.
     *
     * @return The user's information as a [UserDto].
     */
    @PutMapping("/me/email")
    suspend fun changeEmail(@RequestBody payload: ChangeEmailRequest, exchange: ServerWebExchange): ResponseEntity<UserDto> {
        return ResponseEntity.ok().body(
            userSessionService.changeEmail(payload, exchange).toDto()
        )
    }

    /**
     * Change the user's password.
     *
     * @param payload The change password request payload.
     * @param exchange The server web exchange.
     *
     * @return The user's information as a [UserDto].
     */
    @PutMapping("/me/password")
    suspend fun changePassword(@RequestBody payload: ChangePasswordRequest, exchange: ServerWebExchange): ResponseEntity<UserDto> {
        return ResponseEntity.ok().body(
            userSessionService.changePassword(payload, exchange).toDto()
        )
    }

    /**
     * Change the user's information.
     *
     * @param payload The change user request payload.
     * @return The user's information as a [UserDto].
     */
    @PutMapping("/me")
    suspend fun changeUser(@RequestBody payload: ChangeUserRequest): ResponseEntity<UserDto> {
        return ResponseEntity.ok().body(
            userSessionService.changeUser(payload).toDto()
        )
    }

    /**
     * Get the application information.
     *
     * @return The application information as an [ApplicationInfoDto].
     */
    @GetMapping("/me/app")
    suspend fun getApplicationInfo(): ResponseEntity<ApplicationInfoDto> {
        return ResponseEntity.ok().body(
            userSessionService.getApplicationInfo()
        )
    }

    /**
     * Set up two-factor authentication for the user.
     *
     * @param deviceInfo The device info request payload.
     * @return The user's information as a [UserDto].
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
     * @return The user's information as a [UserDto].
     */
    @PostMapping("/refresh")
    suspend fun refreshToken(
        exchange: ServerWebExchange,
        @RequestBody deviceInfoDto: DeviceInfoRequest
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

    /**
     * Delete the user's account.
     *
     * @return A response indicating the success of the operation.
     */
    @DeleteMapping("/me")
    suspend fun delete(): ResponseEntity<Map<String, String>> {
        val clearAccessTokenCookie = cookieService.clearAccessTokenCookie()
        val clearRefreshTokenCookie = cookieService.clearRefreshTokenCookie()
        val clearStepUpTokenCookie = cookieService.clearStepUpCookie()

        userSessionService.deleteUser()

        return ResponseEntity.ok()
            .header("Set-Cookie", clearAccessTokenCookie.toString())
            .header("Set-Cookie", clearRefreshTokenCookie.toString())
            .header("Set-Cookie", clearStepUpTokenCookie.toString())
            .body(mapOf("message" to "success"))
    }
}
