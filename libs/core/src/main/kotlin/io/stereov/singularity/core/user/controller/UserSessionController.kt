package io.stereov.singularity.core.user.controller

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.core.auth.service.AuthenticationService
import io.stereov.singularity.core.auth.service.CookieService
import io.stereov.singularity.core.user.dto.UserResponse
import io.stereov.singularity.core.user.dto.request.*
import io.stereov.singularity.core.user.dto.response.LoginResponse
import io.stereov.singularity.core.user.service.UserSessionService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
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
     * @return The current user's information as a [UserResponse].
     */
    @GetMapping("/me")
    suspend fun getUser(): ResponseEntity<UserResponse> {
        val user = authenticationService.getCurrentUser()

        return ResponseEntity.ok(user.toResponse())
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
                .body(LoginResponse(true, user.toResponse()))
        }

        val accessTokenCookie = cookieService.createAccessTokenCookie(user.id, payload.device.id)
        val refreshTokenCookie = cookieService.createRefreshTokenCookie(user.id, payload.device, exchange)

        return ResponseEntity.ok()
            .header("Set-Cookie", accessTokenCookie.toString())
            .header("Set-Cookie", refreshTokenCookie.toString())
            .body(LoginResponse(false, user.toResponse()))
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
    ): ResponseEntity<UserResponse> {
        logger.info { "Executing register" }

        val user = userSessionService.registerAndGetUser(payload, sendEmail)

        val accessTokenCookie = cookieService.createAccessTokenCookie(user.id, payload.device.id)
        val refreshTokenCookie = cookieService.createRefreshTokenCookie(user.id, payload.device, exchange)

        return ResponseEntity.ok()
            .header("Set-Cookie", accessTokenCookie.toString())
            .header("Set-Cookie", refreshTokenCookie.toString())
            .body(user.toResponse())
    }

    /**
     * Verify the two-factor authentication code.
     *
     * @param payload The two-factor authentication request payload.
     * @param exchange The server web exchange.
     *
     * @return The user's information as a [UserResponse].
     */
    @PutMapping("/me/email")
    suspend fun changeEmail(@RequestBody payload: ChangeEmailRequest, exchange: ServerWebExchange): ResponseEntity<UserResponse> {
        return ResponseEntity.ok().body(
            userSessionService.changeEmail(payload, exchange).toResponse()
        )
    }

    /**
     * Change the user's password.
     *
     * @param payload The change password request payload.
     * @param exchange The server web exchange.
     *
     * @return The user's information as a [UserResponse].
     */
    @PutMapping("/me/password")
    suspend fun changePassword(@RequestBody payload: ChangePasswordRequest, exchange: ServerWebExchange): ResponseEntity<UserResponse> {
        return ResponseEntity.ok().body(
            userSessionService.changePassword(payload, exchange).toResponse()
        )
    }

    /**
     * Change the user's information.
     *
     * @param payload The change user request payload.
     * @return The user's information as a [UserResponse].
     */
    @PutMapping("/me")
    suspend fun changeUser(@RequestBody payload: ChangeUserRequest): ResponseEntity<UserResponse> {
        return ResponseEntity.ok().body(
            userSessionService.changeUser(payload).toResponse()
        )
    }

    @PutMapping("/me/avatar")
    suspend fun setAvatar(@RequestPart file: FilePart): ResponseEntity<UserResponse> {
        return ResponseEntity.ok().body(
            userSessionService.setAvatar(file)
        )
    }

    @DeleteMapping("/me/avatar")
    suspend fun deleteAvatar(): ResponseEntity<UserResponse> {
        return ResponseEntity.ok().body(
            userSessionService.deleteAvatar()
        )
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
