package io.stereov.singularity.auth.twofactor.controller

import io.stereov.singularity.auth.core.component.CookieCreator
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.token.AccessTokenService
import io.stereov.singularity.auth.core.service.token.RefreshTokenService
import io.stereov.singularity.auth.core.service.token.StepUpTokenService
import io.stereov.singularity.auth.twofactor.dto.request.TwoFactorVerifySetupRequest
import io.stereov.singularity.auth.twofactor.dto.response.TwoFactorRecoveryResponse
import io.stereov.singularity.auth.twofactor.dto.response.TwoFactorSetupResponse
import io.stereov.singularity.auth.twofactor.model.token.TwoFactorTokenType
import io.stereov.singularity.auth.twofactor.service.TotpAuthenticationService
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.core.mapper.UserMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange

@RestController
@RequestMapping("/api/auth/2fa/totp")
@Tag(
    name = "TOTP Authentication",
    description = "Operations related to TOTP."
)
class TotpAuthenticationController(
    private val totpAuthenticationService: TotpAuthenticationService,
    private val cookieCreator: CookieCreator,
    private val accessTokenService: AccessTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val stepUpTokenService: StepUpTokenService,
    private val userMapper: UserMapper,
    private val authProperties: AuthProperties
) {

    @Operation(
        summary = "Get 2FA secret and recovery codes",
        description = "Get a 2FA secret, recovery codes and a TOTP URL. " +
                "This information will be stored inside a token that you will get in the response. " +
                "Use this token to perform the validation.",
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(name = OpenApiConstants.TWO_FACTOR_INIT_SETUP_HEADER),
            SecurityRequirement(name = OpenApiConstants.TWO_FACTOR_INIT_SETUP_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Success.",
                content = [Content(schema = Schema(implementation = TwoFactorSetupResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Not authenticated.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @GetMapping("/setup")
    suspend fun setupTwoFactor(): ResponseEntity<TwoFactorSetupResponse> {
        val res = totpAuthenticationService.setUpTwoFactorAuth()

        return ResponseEntity.ok().body(res)
    }

    @PostMapping("/setup")
    suspend fun validateTwoFactorSetup(
        @RequestBody setupRequest: TwoFactorVerifySetupRequest
    ): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(
            totpAuthenticationService.validateSetup(setupRequest.token, setupRequest.code)
        )
    }

    @DeleteMapping
    suspend fun disableTwoFactorAuth(): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(totpAuthenticationService.disable())
    }

    @PostMapping("/recover")
    suspend fun recoverUser(
        @RequestParam("code") code: String,
        exchange: ServerWebExchange,
        @RequestBody session: SessionInfoRequest
    ): ResponseEntity<TwoFactorRecoveryResponse> {
        val user = totpAuthenticationService.recoverUser(exchange, code)

        val clearTwoFactorCookie = cookieCreator.clearCookie(TwoFactorTokenType.Authentication)
        val accessToken = accessTokenService.create(user.id, session.id)
        val refreshToken = refreshTokenService.create(user.id, session, exchange)
        val stepUpToken = stepUpTokenService.createForRecovery(user.id, session.id, exchange)

        val res = TwoFactorRecoveryResponse(
            userMapper.toResponse(user),
            if (authProperties.allowHeaderAuthentication) accessToken.value else null,
            if (authProperties.allowHeaderAuthentication) refreshToken.value else null,
            if (authProperties.allowHeaderAuthentication) stepUpToken.value else null
        )

        return ResponseEntity.ok()
            .header("Set-Cookie", clearTwoFactorCookie.toString())
            .header("Set-Cookie", cookieCreator.createCookie(accessToken).toString())
            .header("Set-Cookie", cookieCreator.createCookie(refreshToken).toString())
            .header("Set-Cookie", cookieCreator.createCookie(stepUpToken).toString())
            .body(res)
    }
}