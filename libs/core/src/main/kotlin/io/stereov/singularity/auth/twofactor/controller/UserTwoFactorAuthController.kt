package io.stereov.singularity.auth.twofactor.controller

import io.stereov.singularity.auth.core.exception.model.TwoFactorAuthDisabledException
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.CookieCreator
import io.stereov.singularity.auth.device.dto.DeviceInfoRequest
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.jwt.exception.TokenException
import io.stereov.singularity.auth.session.dto.response.LoginResponse
import io.stereov.singularity.auth.session.service.AccessTokenService
import io.stereov.singularity.auth.session.service.RefreshTokenService
import io.stereov.singularity.auth.twofactor.dto.request.DisableTwoFactorRequest
import io.stereov.singularity.auth.twofactor.dto.request.TwoFactorSetupInitRequest
import io.stereov.singularity.auth.twofactor.dto.request.TwoFactorVerifySetupRequest
import io.stereov.singularity.auth.twofactor.dto.response.*
import io.stereov.singularity.auth.twofactor.model.TwoFactorTokenType
import io.stereov.singularity.auth.twofactor.service.StepUpTokenService
import io.stereov.singularity.auth.twofactor.service.TwoFactorInitSetupTokenService
import io.stereov.singularity.auth.twofactor.service.UserTwoFactorAuthService
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.core.mapper.UserMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange

/**
 * # UserTwoFactorAuthController
 *
 * This controller handles two-factor authentication (2FA) for users.
 * It provides endpoints for setting up 2FA, verifying 2FA codes,
 * checking 2FA status, and recovering user accounts.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@RestController
@RequestMapping("/api/user/2fa")
class UserTwoFactorAuthController(
    private val twoFactorService: UserTwoFactorAuthService,
    private val authProperties: AuthProperties,
    private val geolocationService: GeolocationService,
    private val userMapper: UserMapper,
    private val accessTokenService: AccessTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val setupInitTokenService: TwoFactorInitSetupTokenService,
    private val stepUpTokenService: StepUpTokenService,
    private val cookieCreator: CookieCreator
) {

    @PostMapping("/start-setup")
    @Operation(
        summary = "Initialize 2FA setup",
        description = "Initialize the 2FA by authorizing the current user. If successful, an SetupStartupToken will be set.",
        security = [SecurityRequirement(name = "bearerAuth")],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Authorization successful.",
                content = [Content(schema = Schema(implementation = TwoFactorSetupStartupResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid credentials.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun initTwoFactorAuthSetup(@RequestBody req: TwoFactorSetupInitRequest): ResponseEntity<TwoFactorSetupStartupResponse> {
        val setupInitToken = setupInitTokenService.create(req)

        return ResponseEntity.ok()
            .header("Set-Cookie", cookieCreator.createCookie(setupInitToken).toString())
            .header("Set-Cookie", cookieCreator.createCookie(setupInitToken).toString())
            .body(TwoFactorSetupStartupResponse(if (authProperties.allowHeaderAuthentication) setupInitToken.value else null))
    }

    @Operation(
        summary = "Perform 2FA setup",
        description = "Generate 2FA secret, recovery codes ",
        security = [SecurityRequirement(name = "bearerAuth")],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Authorization successful.",
                content = [Content(schema = Schema(implementation = TwoFactorSetupResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid credentials.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @GetMapping("/setup")
    suspend fun setupTwoFactorAuth(exchange: ServerWebExchange): ResponseEntity<TwoFactorSetupResponse> {
        val res = twoFactorService.setUpTwoFactorAuth(exchange)

        return ResponseEntity.ok().body(res)
    }

    @PostMapping("/setup")
    suspend fun validateTwoFactorSetup(
        @RequestBody setupRequest: TwoFactorVerifySetupRequest
    ): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(
            twoFactorService.validateSetup(setupRequest.token, setupRequest.code)
        )
    }

    @PostMapping("/recovery")
    suspend fun recoverUser(
        @RequestParam("code") code: String,
        exchange: ServerWebExchange,
        @RequestBody device: DeviceInfoRequest
    ): ResponseEntity<TwoFactorRecoveryResponse> {
        val user = twoFactorService.recoverUser(exchange, code)

        val clearTwoFactorCookie = cookieCreator.clearCookie(TwoFactorTokenType.Login)
        val accessToken = accessTokenService.create(user.id, device.id)
        val refreshToken = refreshTokenService.create(user.id, device, exchange)
        val stepUpToken = stepUpTokenService.createForRecovery(user.id, device.id, exchange)

        val res = TwoFactorRecoveryResponse(
            if (authProperties.allowHeaderAuthentication) accessToken.value else null,
            if (authProperties.allowHeaderAuthentication) refreshToken.value else null,
            if (authProperties.allowHeaderAuthentication) stepUpToken.value else null
        )

        return ResponseEntity.ok()
            .header("Set-Cookie", clearTwoFactorCookie.value)
            .header("Set-Cookie", cookieCreator.createCookie(accessToken).toString())
            .header("Set-Cookie", cookieCreator.createCookie(refreshToken).toString())
            .header("Set-Cookie", cookieCreator.createCookie(stepUpToken).toString())
            .body(res)
    }

    @PostMapping("/verify-login")
    suspend fun verifyTwoFactorAuth(
        @RequestParam("code") code: Int,
        exchange: ServerWebExchange,
        @RequestBody device: DeviceInfoRequest
    ): ResponseEntity<LoginResponse> {
        val user = twoFactorService.validateTwoFactorCode(exchange, code)

        val accessToken = accessTokenService.create(user.id, device.id)
        val refreshToken = refreshTokenService.create(user.id, device, exchange)

        val clearTwoFactorCookie = cookieCreator.clearCookie(TwoFactorTokenType.Login)

        val res = LoginResponse(
            false,
            userMapper.toResponse(user),
            if (authProperties.allowHeaderAuthentication) accessToken.value else null,
            if (authProperties.allowHeaderAuthentication) refreshToken.value else null,
            geolocationService.getLocationOrNull(exchange.request)
        )

        return ResponseEntity.ok()
            .header("Set-Cookie", clearTwoFactorCookie.value)
            .header("Set-Cookie", cookieCreator.createCookie(accessToken).toString())
            .header("Set-Cookie", cookieCreator.createCookie(refreshToken).toString())
            .body(res)
    }

    @GetMapping("/login-status")
    suspend fun getTwoFactorAuthStatus(exchange: ServerWebExchange): ResponseEntity<TwoFactorStatusResponse> {
        val isPending = twoFactorService.loginVerificationNeeded(exchange)

        val res = ResponseEntity.ok()

        if (!isPending) {
            val clearTwoFactorTokenCookie = cookieCreator.clearCookie(TwoFactorTokenType.Login)
            res.header("Set-Cookie", clearTwoFactorTokenCookie.value)
        }

        return res.body(TwoFactorStatusResponse(isPending))
    }

    /**
     * Set the step-up authentication status.
     *
     * @param code The step-up authentication code.
     *
     * @return A response indicating the success of the operation.
     */
    @PostMapping("/verify-step-up")
    suspend fun setStepUp(@RequestParam code: Int): ResponseEntity<TwoFactorStepUpResponse> {
        val stepUpTokenCookie = stepUpTokenService.create(code)

        return ResponseEntity.ok()
            .header("Set-Cookie", cookieCreator.createCookie(stepUpTokenCookie).toString())
            .body(TwoFactorStepUpResponse(if (authProperties.allowHeaderAuthentication) stepUpTokenCookie.value else null))
    }

    /**
     * Get the step-up authentication status.
     *
     * @param exchange The server web exchange.
     *
     * @return The step-up authentication status as a [TwoFactorStatusResponse].
     */
    @GetMapping("/step-up-status")
    suspend fun getStepUpStatus(exchange: ServerWebExchange): ResponseEntity<TwoFactorStatusResponse> {
        val twoFactorRequired = try {
            stepUpTokenService.extract(exchange)
            false
        } catch (_: TokenException) {
            true
        } catch (_: TwoFactorAuthDisabledException) {
            false
        }

        return ResponseEntity.ok(TwoFactorStatusResponse(twoFactorRequired))
    }

    /**
     * Disable two-factor authentication for the user.
     * This requires a step-up authentication token.
     *
     * @param exchange The server web exchange.
     *
     * @return The updated user information as a [UserResponse].
     */
    @PostMapping("/disable")
    suspend fun disableTwoFactorAuth(exchange: ServerWebExchange, @RequestBody req: DisableTwoFactorRequest): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(twoFactorService.disable(exchange, req))
    }
}
