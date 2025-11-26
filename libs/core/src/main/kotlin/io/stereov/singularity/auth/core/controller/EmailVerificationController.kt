package io.stereov.singularity.auth.core.controller

import io.stereov.singularity.auth.core.dto.response.MailCooldownResponse
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.core.service.EmailVerificationService
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.global.model.SendEmailResponse
import io.stereov.singularity.principal.core.dto.response.UserResponse
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/auth/email/verification")
@Tag(
    name = "Authentication",
)
class EmailVerificationController(
    private val emailVerificationService: EmailVerificationService,
    private val authorizationService: AuthorizationService,
) {

    @PostMapping
    @Operation(
        summary = "Verify Email",
        description = """
            Verify the user's email address using the `token` the user received a email verification email.
            You can find more information about this [here](https://singularity.stereov.io/docs/guides/auth/authentication#email-verification).
            
            When registering a user and [email is enabled in your application](https://singularity.stereov.io/docs/guides/email/configuration)
            an email containing a link is sent to the user's email address.
            This link should point to the frontend of your application.
            Your frontend should extract the token from the URL and send it to this endpoint with the
            token as request parameter.
            You can find more information about this [here](https://singularity.stereov.io/docs/guides/auth/authentication#email-verification).
            
            You can resend this email through the endpoint [`POST /api/auth/email/verification/send`](https://singularity.stereov.io/docs/api/send-email-verification-email).

            ### Locale
            
            A locale can be specified for this request. 
            The email will be sent in the specified locale.
            You can learn more about locale in emails [here](https://singularity.stereov.io/docs/guides/email/templates).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).

            >**Note:** If email is disabled, there is no way to verify a user's email address.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/docs/auth/authentication#email-verification"),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Updated user information.",
            ),
            ApiResponse(
                responseCode = "304",
                description = "Trying to verify an email for verified account.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Trying to verify an email for [`GUEST`](https://singularity.stereov.io/docs/guides/auth/roles#guests).",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired verification token.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun verifyEmail(
        @RequestParam token: String,
        @RequestParam locale: Locale?
    ): ResponseEntity<UserResponse> {
        val authInfo = emailVerificationService.verifyEmail(token, locale)

        return ResponseEntity.ok()
            .body(authInfo)
    }

    @GetMapping("/cooldown")
    @Operation(
        summary = "Get Remaining Email Verification CooldownActive",
        description = """
            Get the remaining time in seconds until you can send another email verification email.
            
            You can find more information about email verification [here](https://singularity.stereov.io/docs/guides/auth/authentication#email-verification).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/docs/auth/authentication#email-verification"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The remaining cooldown.",
            ),
            ApiResponse(
                responseCode = "401",
                description = "AccessToken is invalid or expired.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
        ]
    )
    suspend fun getRemainingEmailVerificationCooldown(): ResponseEntity<MailCooldownResponse> {
        val email = authorizationService.getUser().sensitive.email
        val remainingCooldown = if (email != null) {
            emailVerificationService.getRemainingCooldown(email)
        } else 0

        return ResponseEntity.ok().body(MailCooldownResponse(remainingCooldown))
    }

    @PostMapping("/send")
    @Operation(
        summary = "Send Email Verification Email",
        description = """
            Send an email verification email to the user.
            
            This endpoint is for **resending** the verification email only.
            When registering a user and [email is enabled in your application](https://singularity.stereov.io/docs/guides/email/configuration)
            an email containing a link is automatically sent to the user's email address.
            This link should point to the frontend of your application.
            You can find more information about this [here](https://singularity.stereov.io/docs/guides/auth/authentication#email-verification).
            Your frontend should extract the token from the URL and send it to this endpoint with the
            token as request parameter.
            
            You can perform the verification using the token through the endpoint [`POST /api/auth/email/verification`](https://singularity.stereov.io/docs/api/verify-email).
            
            >**Note:** If email is disabled, there is no way to verify a user's email address.
            
            If there is no account associated with the given email address, 
            a [No Principal Information](https://singularity.stereov.io/docs/guides/auth/security-alerts#no-account-information)
            email will be sent to the given email address.
            
            ### Locale
            
            A locale can be specified for this request. 
            The email will be sent in the specified locale.
            You can learn more about locale in emails [here](https://singularity.stereov.io/docs/guides/email/templates).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).

            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
            
            >**Note:** After each email, a cooldown will be started.
            >You can check the status of the cooldown through the endpoint [`GET /api/auth/email/verification/cooldown`](https://singularity.stereov.io/docs/api/get-remaining-email-verification-cooldown).
            >When the cooldown is active, no new verification email can be sent.
            >The cooldown can be configured [here](https://singularity.stereov.io/docs/guides/email/configuration).
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/docs/auth/authentication#email-verification"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The number of seconds the user needs to wait before sending a new email.",
            ),
            ApiResponse(
                responseCode = "304",
                description = "Trying to send a verification email for verified account.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Trying to send a verification email for [`GUEST`](https://singularity.stereov.io/docs/guides/auth/roles#guests).",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "AccessToken is invalid or expired.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "429",
                description = "CooldownActive is active. You have to wait until you can send another email.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "503",
                description = "Email is disabled in your application.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
        ]
    )
    suspend fun sendEmailVerificationEmail(
        @RequestParam locale: Locale?,
    ): ResponseEntity<SendEmailResponse> {
        val user = authorizationService.getUser()
        val cooldown = emailVerificationService.sendVerificationEmail(user, locale)

        return ResponseEntity.ok().body(
            SendEmailResponse(cooldown)
        )
    }
}
