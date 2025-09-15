package io.stereov.singularity.auth.twofactor.controller

import io.stereov.singularity.auth.core.component.CookieCreator
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.exception.model.UserAlreadyAuthenticatedException
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.core.service.token.AccessTokenService
import io.stereov.singularity.auth.core.service.token.RefreshTokenService
import io.stereov.singularity.auth.core.service.token.SessionTokenService
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
import java.util.*

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
    private val authProperties: AuthProperties,
    private val sessionTokenService: SessionTokenService,
    private val authorizationService: AuthorizationService
) {

    @Operation(
        summary = "Get 2FA secret and recovery codes",
        description = "Get a 2FA secret, recovery codes and a TOTP URL. " +
                "This information will be stored inside a token that you will get in the response. " +
                "Use this token to perform the validation.",
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE)
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
    suspend fun getTotpDetails(): ResponseEntity<TwoFactorSetupResponse> {
        val res = totpAuthenticationService.getTotpDetails()

        return ResponseEntity.ok().body(res)
    }

    @PostMapping("/setup")
    @Operation(
        summary = "Set up TOTP",
        description = "Set up TOTP for a user using a TOTPSetupToken and the TOTP code.",
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Success.",
                content = [Content(schema = Schema(implementation = TwoFactorSetupResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Wrong code or not authorized.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun setUpTotp(
        @RequestBody setupRequest: TwoFactorVerifySetupRequest
    ): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(
            totpAuthenticationService.validateSetup(setupRequest.token, setupRequest.code)
        )
    }

    @DeleteMapping
    @Operation(
        summary = "Disable TOTP",
        description = "Disable TOTP for the current user.",
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Success.",
                content = [Content(schema = Schema(implementation = UserResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Not authorized.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun disableTwoFactorAuth(): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(totpAuthenticationService.disable())
    }

    @PostMapping("/recover")
    @Operation(
        summary = "User recovery",
        description = "Recover the user if they lost access to their 2FA device. " +
                "After successful recovery, an AccessToken, RefreshToken and StepUpToken will be set " +
                "as HTTP-only cookies and returned in the response body if header authentication is enabled.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Recovery successful. If header authentication is enabled, " +
                        "the response will contain all tokens.",
                content = [Content(schema = Schema(implementation = TwoFactorRecoveryResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Wrong code.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "User is already authenticated.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
        ]
    )
    suspend fun recoverUser(
        @RequestParam("code") code: String,
        exchange: ServerWebExchange,
        @RequestBody session: SessionInfoRequest?
    ): ResponseEntity<TwoFactorRecoveryResponse> {
        if (authorizationService.isAuthenticated())
            throw UserAlreadyAuthenticatedException("Recovery failed: user is already authenticated")

        val user = totpAuthenticationService.recoverUser(exchange, code)
        val sessionId = UUID.randomUUID()

        val clearTwoFactorCookie = cookieCreator.clearCookie(TwoFactorTokenType.Authentication)
        val sessionToken = sessionTokenService.create(sessionInfo = session)
        val accessToken = accessTokenService.create(user, sessionId)
        val refreshToken = refreshTokenService.create(user.id, sessionId,session, exchange)
        val stepUpToken = stepUpTokenService.createForRecovery(user.id, sessionId, exchange)

        val res = TwoFactorRecoveryResponse(
            userMapper.toResponse(user),
            if (authProperties.allowHeaderAuthentication) accessToken.value else null,
            if (authProperties.allowHeaderAuthentication) refreshToken.value else null,
            if (authProperties.allowHeaderAuthentication) stepUpToken.value else null,
            if (authProperties.allowHeaderAuthentication) sessionToken.value else null,
        )

        return ResponseEntity.ok()
            .header("Set-Cookie", clearTwoFactorCookie.toString())
            .header("Set-Cookie", cookieCreator.createCookie(accessToken).toString())
            .header("Set-Cookie", cookieCreator.createCookie(refreshToken).toString())
            .header("Set-Cookie", cookieCreator.createCookie(stepUpToken).toString())
            .header("Set-Cookie", cookieCreator.createCookie(sessionToken).toString())
            .body(res)
    }
}
