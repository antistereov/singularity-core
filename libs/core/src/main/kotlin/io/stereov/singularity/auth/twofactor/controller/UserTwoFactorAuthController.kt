package io.stereov.singularity.auth.twofactor.controller

import io.stereov.singularity.auth.core.exception.model.TwoFactorAuthDisabledException
import io.stereov.singularity.auth.core.service.AuthenticationService
import io.stereov.singularity.auth.core.service.CookieService
import io.stereov.singularity.auth.device.dto.DeviceInfoRequest
import io.stereov.singularity.auth.jwt.exception.TokenException
import io.stereov.singularity.auth.twofactor.dto.request.DisableTwoFactorRequest
import io.stereov.singularity.auth.twofactor.dto.request.TwoFactorStartSetupRequest
import io.stereov.singularity.auth.twofactor.dto.request.TwoFactorVerifySetupRequest
import io.stereov.singularity.auth.twofactor.dto.response.TwoFactorSetupResponse
import io.stereov.singularity.auth.twofactor.dto.response.TwoFactorStatusResponse
import io.stereov.singularity.auth.twofactor.service.UserTwoFactorAuthService
import io.stereov.singularity.global.model.SuccessResponse
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.core.mapper.UserMapper
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
    private val cookieService: CookieService,
    private val authenticationService: AuthenticationService,
    private val userMapper: UserMapper
) {

    @PostMapping("/start-setup")
    suspend fun startTwoFactorAuthSetup(@RequestBody req: TwoFactorStartSetupRequest): ResponseEntity<SuccessResponse> {
        val setupCookie = cookieService.createTwoFactorSetupCookie(req)

        return ResponseEntity.ok()
            .header("Set-Cookie", setupCookie.toString())
            .body(SuccessResponse(true))
    }

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
    ): ResponseEntity<UserResponse> {
        val user = twoFactorService.recoverUser(exchange, code)

        val clearTwoFactorCookie = cookieService.clearLoginVerificationCookie()
        val accessTokenCookie = cookieService.createAccessTokenCookie(user.id, device.id)
        val refreshTokenCookie = cookieService.createRefreshTokenCookie(user.id, device, exchange)
        val stepUpTokenCookie = cookieService.createStepUpCookieForRecovery(user.id, device.id, exchange)

        return ResponseEntity.ok()
            .header("Set-Cookie", clearTwoFactorCookie.toString())
            .header("Set-Cookie", accessTokenCookie.toString())
            .header("Set-Cookie", refreshTokenCookie.toString())
            .header("Set-Cookie", stepUpTokenCookie.toString())
            .body(userMapper.toResponse(user))
    }

    @PostMapping("/verify-login")
    suspend fun verifyTwoFactorAuth(
        @RequestParam("code") code: Int,
        exchange: ServerWebExchange,
        @RequestBody device: DeviceInfoRequest
    ): ResponseEntity<UserResponse> {
        val user = twoFactorService.validateTwoFactorCode(exchange, code)

        val accessTokenCookie = cookieService.createAccessTokenCookie(user.id, device.id)
        val refreshTokenCookie = cookieService.createRefreshTokenCookie(user.id, device, exchange)

        val clearTwoFactorCookie = cookieService.clearLoginVerificationCookie()
        return ResponseEntity.ok()
            .header("Set-Cookie", clearTwoFactorCookie.toString())
            .header("Set-Cookie", accessTokenCookie.toString())
            .header("Set-Cookie", refreshTokenCookie.toString())
            .body(userMapper.toResponse(user))
    }

    @GetMapping("/login-status")
    suspend fun getTwoFactorAuthStatus(exchange: ServerWebExchange): ResponseEntity<TwoFactorStatusResponse> {
        val isPending = twoFactorService.loginVerificationNeeded(exchange)

        val res = ResponseEntity.ok()

        if (!isPending) {
            val clearTwoFactorTokenCookie = cookieService.clearLoginVerificationCookie()
            res.header("Set-Cookie", clearTwoFactorTokenCookie.toString())
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
    suspend fun setStepUp(@RequestParam code: Int): ResponseEntity<UserResponse> {
        val user = authenticationService.getCurrentUser()
        val stepUpTokenCookie = cookieService.createStepUpCookie(code)

        return ResponseEntity.ok()
            .header("Set-Cookie", stepUpTokenCookie.toString())
            .body(userMapper.toResponse(user))
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
            cookieService.validateStepUpCookie(exchange)
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
