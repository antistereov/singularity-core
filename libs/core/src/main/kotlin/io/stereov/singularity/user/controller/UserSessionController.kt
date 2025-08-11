package io.stereov.singularity.user.controller

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.properties.AuthProperties
import io.stereov.singularity.auth.service.AuthenticationService
import io.stereov.singularity.auth.service.CookieService
import io.stereov.singularity.content.translate.model.Language
import io.stereov.singularity.user.dto.UserResponse
import io.stereov.singularity.user.dto.request.*
import io.stereov.singularity.user.dto.response.LoginResponse
import io.stereov.singularity.user.dto.response.RegisterResponse
import io.stereov.singularity.user.service.UserService
import io.stereov.singularity.user.service.UserSessionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "User Session", description = "Operations related to login and session management")
class UserSessionController(
    private val authenticationService: AuthenticationService,
    private val userSessionService: UserSessionService,
    private val cookieService: CookieService,
    private val userService: UserService,
    private val authProperties: AuthProperties
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

        return ResponseEntity.ok(userService.createResponse(user))
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
                .body(LoginResponse(true, userService.createResponse(user)))
        }

        val accessTokenCookie = cookieService.createAccessTokenCookie(user.id, payload.device.id)
        val refreshTokenCookie = cookieService.createRefreshTokenCookie(user.id, payload.device, exchange)

        val res = LoginResponse(
            false,
            userService.createResponse(user),
            if (authProperties.allowHeaderAuthentication) accessTokenCookie.value else null
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
            userService.createResponse(user),
            if (authProperties.allowHeaderAuthentication) accessTokenCookie.value else null
        )

        return ResponseEntity.ok()
            .header("Set-Cookie", accessTokenCookie.toString())
            .header("Set-Cookie", refreshTokenCookie.toString())
            .body(res)
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
    suspend fun changeEmail(
        @RequestBody payload: ChangeEmailRequest,
        @RequestParam lang: Language = Language.EN,
        exchange: ServerWebExchange
    ): ResponseEntity<UserResponse> {
        val user = userSessionService.changeEmail(payload, exchange, lang)
        return ResponseEntity.ok().body(
            userService.createResponse(user)
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
        val user = userSessionService.changePassword(payload, exchange)
        
        return ResponseEntity.ok().body(
            userService.createResponse(user)
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
        val user = userSessionService.changeUser(payload)
        
        return ResponseEntity.ok().body(
            userService.createResponse(user)
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
