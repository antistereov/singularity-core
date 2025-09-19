package io.stereov.singularity.auth.core.controller

import io.stereov.singularity.auth.core.dto.request.ResetPasswordRequest
import io.stereov.singularity.auth.core.dto.request.SendPasswordResetRequest
import io.stereov.singularity.auth.core.dto.response.MailCooldownResponse
import io.stereov.singularity.auth.core.service.PasswordResetService
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.MailSendResponse
import io.stereov.singularity.global.model.SuccessResponse
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/auth/password")
@Tag(
    name = "Authentication"
)
class PasswordResetController(
    private val passwordResetService: PasswordResetService
) {


    @PostMapping("/reset")
    @Operation(
        summary = "Reset Password",
        description = """
            Perform a password reset using a `token` you obtained the user received in a password reset email.
            You can learn more about the password reset [here](https://singularity.stereov.io/docs/guides/auth/authentication#password-reset).
            
            When request a password reset through [`POST /api/auth/password/reset-request`](https://singularity.stereov.io/docs/api/send-password-reset-email) 
            and [email is enabled in your application](https://singularity.stereov.io/docs/guides/email/configuration) 
            an email containing a link is sent to the user's email address.
            This link should point to the frontend of your application.
            Your frontend should extract the token from the URL and send it to this endpoint with the
            token as request parameter.
            You can find more information about this [here](https://singularity.stereov.io/docs/guides/auth/authentication#password-reset).
            
            If successful, the user can log in using the new password afterwards.
            
            You can resend this email through the endpoint [`POST /api/auth/password/reset-request`](https://singularity.stereov.io/docs/api/send-password-reset-email).

            **Note:** If email is not enabled, there is no way to reset the password.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/authentication#password-reset"),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Updated user information.",
            ),
            ApiResponse(
                responseCode = "401",
                description = "Token is invalid.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "No password identity is set up for the user.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
        ]
    )
    suspend fun resetPassword(
        @RequestParam token: String,
        @RequestBody req: ResetPasswordRequest
    ): ResponseEntity<SuccessResponse> {
        passwordResetService.resetPassword(token, req)

        return ResponseEntity.ok()
            .body(SuccessResponse())
    }

    @GetMapping("/reset/cooldown")
    @Operation(
        summary = "Get Remaining Password Reset Cooldown",
        description = """
            Get the remaining time in seconds until you can send another password reset request.
            
            You can find more information about a password reset [here](https://singularity.stereov.io/docs/guides/auth/authentication#password-reset).
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/authentication#password-reset"),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The remaining cooldown.",
            ),
        ]
    )
    suspend fun getRemainingPasswordResetCooldown(
        @RequestParam email: String
    ): ResponseEntity<MailCooldownResponse> {
        val remainingCooldown = passwordResetService.getRemainingCooldown(email)

        return ResponseEntity.ok().body(MailCooldownResponse(remainingCooldown))
    }

    @PostMapping("/reset-request")
    @Operation(
        summary = "Send Password Reset Email",
        description = """
            Send a password reset request email to the user's email.
            You can learn more about the password reset [here](https://singularity.stereov.io/docs/guides/auth/authentication#password-reset).
            
            When request a password reset through this endpoint
            and [email is enabled in your application](https://singularity.stereov.io/docs/guides/email/configuration) 
            an email containing a link is sent to the user's email address.
            This link should point to the frontend of your application.
            Your frontend should extract the token from the URL and send it to this endpoint with the
            token as request parameter.
            You can find more information about this [here](https://singularity.stereov.io/docs/guides/auth/authentication#password-reset).
            
            You can perform the reset using the token through the endpoint [`POST /api/auth/password/reset`](https://singularity.stereov.io/docs/api/reset-password).

            **Note:** If email is not enabled, there is no way to reset the password.
            
            **Note:** After each email, a cooldown will be started.
            When the cooldown is active, no new verification email can be sent.
            The cooldown can be configured [here](https://singularity.stereov.io/docs/guides/email/configuration).
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/authentication#password-reset"),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The number of seconds the user needs to wait to send a new email.",
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "429",
                description = "Cooldown is active. You have to wait until you can send another email.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "503",
                description = "Email is disabled in your application.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
        ]
    )
    suspend fun sendPasswordResetEmail(
        @RequestBody req: SendPasswordResetRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<MailSendResponse> {
        passwordResetService.sendPasswordReset(req, locale)

        return ResponseEntity.ok().body(
            MailSendResponse(passwordResetService.getRemainingCooldown(req).remaining)
        )
    }
}
