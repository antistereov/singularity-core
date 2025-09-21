package io.stereov.singularity.auth.twofactor.controller

import io.stereov.singularity.auth.core.component.CookieCreator
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.properties.AuthProperties
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
import io.swagger.v3.oas.annotations.ExternalDocumentation
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
    name = "Two-Factor Authentication"
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
) {

    @GetMapping("/setup")
    @Operation(
        summary = "Get TOTP Setup Details",
        description = """
            Get a TOTP secret, recovery codes and a TOTP URL.
            The user needs to save the recovery codes and use the URL or the secret to set up 2FA in their 2FA app.
            
            This secret will be stored inside the token contained in the response.
            This token is required to enable TOTP.
            Performing this request will not change the user's state in the database.
            Therefore, the token is the single point of truth for validation.
            Every request will generate a new TOTP secret, new recovery codes and a new TOTP URL.
            
            This action requires a valid `StepUpToken`.
            
            The user must be able to authenticate using a password.
            If the user registered using OAuth2 and did not [set up password authentication](https://singularity.stereov.io/docs/guides/auth/oauth2#adding-password-authentication),
            this action will fail.
    
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/two-factor#setup"),
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_COOKIE),
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = """
                    The TOTP secret, recovery codes, TOTP URL and setup token.
                    
                    Show the secret, TOTP URL to the user. 
                    They can use the secret or URL to set up 2FA in their 2FA app.
                    
                    Make sure the user saves the recovery codes in case they lost access to their 2FA app.
                    
                    The token is required to enable TOTP.
                """,
                content = [Content(schema = Schema(implementation = TwoFactorSetupResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "AccessToken or StepUpToken is invalid.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "The user did not configure authentication using password.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "The user already enabled TOTP.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun getTotpSetupDetails(): ResponseEntity<TwoFactorSetupResponse> {
        val res = totpAuthenticationService.getTotpDetails()

        return ResponseEntity.ok().body(res)
    }

    @PostMapping("/setup")
    @Operation(
        summary = "Enable TOTP as 2FA Method",
        description = "Set up TOTP for a user using a TOTPSetupToken and the TOTP code.",
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/two-factor#setup"),
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
                description = "AccessToken or StepUpToken is invalid.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "The user did not configure authentication using password.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "The user already enabled TOTP.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun enableTotpAsTwoFactorMethod(
        @RequestBody setupRequest: TwoFactorVerifySetupRequest
    ): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(
            totpAuthenticationService.validateSetup(setupRequest.token, setupRequest.code)
        )
    }

    @DeleteMapping
    @Operation(
        summary = "Disable TOTP as 2FA Method",
        description = "Disable TOTP for the current user.",
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/two-factor#disable"),
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
    suspend fun disableTotpAsTwoFactorMethod(): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(totpAuthenticationService.disable())
    }

    @PostMapping("/recover")
    @Operation(
        summary = "Recover From TOTP",
        description = "Recover the user if they lost access to their 2FA device. " +
                "After successful recovery, an AccessToken, RefreshToken and StepUpToken will be set " +
                "as HTTP-only cookies and returned in the response body if header authentication is enabled.",
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/two-factor#recovery"),
        security = [
            SecurityRequirement(name = OpenApiConstants.TWO_FACTOR_AUTHENTICATION_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.TWO_FACTOR_AUTHENTICATION_TOKEN_COOKIE)
        ],
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
    suspend fun recoverFromTotp(
        @RequestParam("code") code: String,
        exchange: ServerWebExchange,
        @RequestBody session: SessionInfoRequest?
    ): ResponseEntity<TwoFactorRecoveryResponse> {

        val user = totpAuthenticationService.recoverUser(exchange, code)
        val sessionId = UUID.randomUUID()

        val clearTwoFactorCookie = cookieCreator.clearCookie(TwoFactorTokenType.Authentication)
        val sessionToken = sessionTokenService.create(sessionInfo = session)
        val accessToken = accessTokenService.create(user, sessionId)
        val refreshToken = refreshTokenService.create(user, sessionId,session, exchange)
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
