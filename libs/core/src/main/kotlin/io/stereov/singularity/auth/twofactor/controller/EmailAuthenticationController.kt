package io.stereov.singularity.auth.twofactor.controller

import io.stereov.singularity.auth.core.dto.response.MailCooldownResponse
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.twofactor.dto.request.EnableEmailTwoFactorMethodRequest
import io.stereov.singularity.auth.twofactor.service.EmailAuthenticationService
import io.stereov.singularity.auth.token.service.TwoFactorAuthenticationTokenService
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.SendEmailResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.global.model.SuccessResponse
import io.stereov.singularity.principal.core.dto.response.UserResponse
import io.stereov.singularity.principal.core.mapper.PrincipalMapper
import io.stereov.singularity.principal.core.service.UserService
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
@RequestMapping("/api/auth/2fa/email")
@Tag(
    name = "Two-Factor Authentication"
)
class EmailAuthenticationController(
    private val emailAuthenticationService: EmailAuthenticationService,
    private val authorizationService: AuthorizationService,
    private val principalMapper: PrincipalMapper,
    private val twoFactorAuthenticationTokenService: TwoFactorAuthenticationTokenService,
    private val userService: UserService
) {

    @PostMapping("/send")
    @Operation(
        summary = "Send Email 2FA Code",
        description = """
            Send an email containing a 2FA code to the user.
            
            Learn more about email as 2FA method [here](https://singularity.stereov.io/docs/guides/auth/two-factor#email).
            
            This endpoint is used to send a 2FA email code for enabling email as a 2FA method
            and to resend 2FA codes when authenticating user that already enabled email as a 2FA method.
            
            >**Note:** Each request will generate a new code and invalidate all old codes.
            
            If email is the preferred 2FA method, an email will be sent automatically after successful authentication
            with the user's password.
            You can learn more about preferred 2FA methods [here](https://singularity.stereov.io/docs/guides/auth/two-factor#changing-the-preferred-method).
            
            ### Requirements
            - The user can authenticate using password. 2FA will not work with OAuth2. 
              The OAuth2 provider will validate the second factor if the user enabled it for the provider.
              
            ### Locale
            
            A locale can be specified for this request. 
            The email will be sent in the specified locale.
            You can learn more about locale in emails [here](https://singularity.stereov.io/docs/guides/email/templates).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            ### Tokens
            
            There are two options: 
            1. If email as a 2FA method is disabled, you can request an email with a token to enable it.
               In this case you need a valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token).
               If email is already enabled, this request will return `400 - BAD REQUEST`.
            2. If email as 2FA method is already enabled, you can request a 2FA email code with a valid
               [`TwoFactorAuthenticationToken`](https://singularity.stereov.io/docs/guides/auth/tokens#two-factor-authentication-token).
               
            >**Note:** After each email, a cooldown will be started.
            >When the cooldown is active, no new email can be sent.
            >You can request the remaining cooldown throw the endpoint 
            >[`GET /api/auth/2fa/email/cooldown`](https://singularity.stereov.io/docs/api/get-remaining-email-two-factor-cooldown).
            >The cooldown can be configured [here](https://singularity.stereov.io/docs/guides/email/configuration).
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/two-factor#sending-a-2fa-code-via-email"),
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(name = OpenApiConstants.TWO_FACTOR_AUTHENTICATION_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.TWO_FACTOR_AUTHENTICATION_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Success.",
                content = [Content(schema = Schema(implementation = SuccessResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "2FA cannot be sent for users who didn't configure authentication using a password.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired `AccessToken` or `TwoFactorAuthenticationToken`.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "429",
                description = "Cannot send email while cooldown is active.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "503",
                description = "Email is disabled in your application.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
        ]
    )
    suspend fun sendEmailTwoFactorCode(
        @RequestParam locale: Locale?,
        exchange: ServerWebExchange,
    ): ResponseEntity<SendEmailResponse> {
        val userId = authorizationService.getUserIdOrNull()
            ?: twoFactorAuthenticationTokenService.extract(exchange).userId

        emailAuthenticationService.sendMail(
            userService.findById(userId),
            locale
        )

        return ResponseEntity.ok(SendEmailResponse(emailAuthenticationService.getRemainingCooldown(userId)))
    }

    @PostMapping("/enable")
    @Operation(
        summary = "Enable Email as 2FA Method",
        description = """
            Enable email as 2FA method.
            
            Learn more about email as 2FA method [here](https://singularity.stereov.io/docs/guides/auth/two-factor#email).
            
            A [security alert](https://singularity.stereov.io/docs/guides/auth/security-alerts#2fa-specific-alerts)
            will be sent to the user's email if this setting is enabled and
            email is [enabled and configured correctly](https://singularity.stereov.io/docs/guides/email/configuration).
            
            ### Requirements
            - The user can authenticate using password. 2FA will not work with OAuth2. 
              The OAuth2 provider will validate the second factor if the user enabled it for the provider.
            
            >**Note:** If [email is enabled](https://singularity.stereov.io/docs/guides/email/configuration) in your application,
            >email is 2FA method will be enabled by default for every user that registers with a password.
            
            ### Locale
            
            A locale can be specified for this request. 
            The email will be sent in the specified locale.
            You can learn more about locale in emails [here](https://singularity.stereov.io/docs/guides/email/templates).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
            - A valid [`StepUpToken`](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)
              is required. This token should match user and session contained in the `AccessToken`.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/two-factor#setup-1"),
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Updated user information.",
                content = [Content(schema = Schema(implementation = UserResponse::class))]
            ),
            ApiResponse(
                responseCode = "304",
                description = "This method is already enabled for the user.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "2FA cannot be enabled for users who didn't configure authentication using a password.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid `AccessToken` or `StepUpToken`.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
        ]
    )
    suspend fun enableEmailAsTwoFactorMethod(
        @RequestBody payload: EnableEmailTwoFactorMethodRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(
            principalMapper.toResponse(emailAuthenticationService.enable(payload, locale))
        )
    }

    @DeleteMapping
    @Operation(
        summary = "Disable Email as 2FA Method",
        description = """
            Disable email as 2FA method.
            
            Learn more about email as 2FA method [here](https://singularity.stereov.io/docs/guides/auth/two-factor#email).
            
            A [security alert](https://singularity.stereov.io/docs/guides/auth/security-alerts#2fa-specific-alerts)
            will be sent to the user's email if this setting is enabled and
            email is [enabled and configured correctly](https://singularity.stereov.io/docs/guides/email/configuration).
            
            ### Locale
            
            A locale can be specified for this request. 
            The email will be sent in the specified locale.
            You can learn more about locale in emails [here](https://singularity.stereov.io/docs/guides/email/templates).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
            - A valid [`StepUpToken`](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)
              is required. This token should match user and session contained in the `AccessToken`.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/two-factor#disable-1"),
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Updated user information.",
                content = [Content(schema = Schema(implementation = UserResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "This method is already disabled for the user.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid `AccessToken` or `RefreshToken`.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
        ]
    )
    suspend fun disableEmailAsTwoFactorMethod(
        @RequestParam locale: Locale?
    ): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(
            principalMapper.toResponse(emailAuthenticationService.disable(locale))
        )
    }

    @GetMapping("/cooldown")
    @Operation(
        summary = "Get Remaining Email 2FA Code CooldownActive",
        description = """
            Get the remaining time in seconds until you can send another email containing a 2FA code.
            
            Learn more about email as 2FA method [here](https://singularity.stereov.io/docs/guides/auth/two-factor#email).
            
            ### Tokens
            - Requires either a valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              or [`TwoFactorAuthenticationToken`](https://singularity.stereov.io/docs/guides/auth/tokens#two-factor-authentication-token).
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/two-factor#sending-a-2fa-code-via-email"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(OpenApiConstants.TWO_FACTOR_AUTHENTICATION_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.TWO_FACTOR_AUTHENTICATION_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The remaining cooldown.",
                content = [Content(schema = Schema(implementation = MailCooldownResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired `AccessToken` or `TwoFactorAuthenticationToken`.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
        ]
    )
    suspend fun getRemainingEmailTwoFactorCooldown(
        exchange: ServerWebExchange
    ): ResponseEntity<MailCooldownResponse> {
        val userId = authorizationService.getUserIdOrNull()
            ?: twoFactorAuthenticationTokenService.extract(exchange).userId

        val remainingCooldown = emailAuthenticationService.getRemainingCooldown(userId)

        return ResponseEntity.ok().body(MailCooldownResponse(remainingCooldown))
    }
}
