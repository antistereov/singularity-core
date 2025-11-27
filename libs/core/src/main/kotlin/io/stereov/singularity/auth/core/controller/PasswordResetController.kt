package io.stereov.singularity.auth.core.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.core.dto.request.ResetPasswordRequest
import io.stereov.singularity.auth.core.dto.request.SendPasswordResetRequest
import io.stereov.singularity.auth.core.dto.response.MailCooldownResponse
import io.stereov.singularity.auth.core.exception.ResetPasswordException
import io.stereov.singularity.auth.core.exception.SendPasswordResetException
import io.stereov.singularity.auth.core.service.PasswordResetService
import io.stereov.singularity.auth.token.exception.PasswordResetTokenExtractionException
import io.stereov.singularity.auth.token.service.PasswordResetTokenService
import io.stereov.singularity.cache.exception.CacheException
import io.stereov.singularity.global.annotation.ThrowsDomainError
import io.stereov.singularity.global.model.SendEmailResponse
import io.stereov.singularity.global.model.SuccessResponse
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/auth/password")
@Tag(
    name = "Authentication"
)
class PasswordResetController(
    private val passwordResetService: PasswordResetService,
    private val passwordResetTokenService: PasswordResetTokenService,
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
            
            ### Requirements
            - The `password` must be at least 8 characters long and include at least one uppercase letter, 
              one lowercase letter, one number, and one special character (!@#$%^&*()_+={}[]|\:;'"<>,.?/).
            
            ### Locale
            
            A locale can be specified for this request. 
            The email will be sent in the specified locale.
            You can learn more about locale in emails [here](https://singularity.stereov.io/docs/guides/email/templates).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).

            >**Note:** If email is disabled, there is no way to reset the password.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/authentication#password-reset"),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Success.",
            )
        ]
    )
    @ThrowsDomainError([
        PasswordResetTokenExtractionException::class,
        ResetPasswordException::class,
    ])
    suspend fun resetPassword(
        @RequestParam token: String,
        @RequestBody @Valid req: ResetPasswordRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<SuccessResponse> {
        val token = passwordResetTokenService.extract(token)
            .getOrThrow { when (it) { is PasswordResetTokenExtractionException -> it } }

        passwordResetService.resetPassword(token, req, locale)
            .getOrThrow { when (it) { is ResetPasswordException -> it } }

        return ResponseEntity.ok()
            .body(SuccessResponse())
    }

    @GetMapping("/reset/cooldown")
    @Operation(
        summary = "Get Remaining Password Reset CooldownActive",
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
    @ThrowsDomainError([
        CacheException.Operation::class
    ])
    suspend fun getRemainingPasswordResetCooldown(
        @RequestParam email: String
    ): ResponseEntity<MailCooldownResponse> {
        val remainingCooldown = passwordResetService.getRemainingCooldown(email)
            .getOrThrow { when (it) { is CacheException.Operation -> it } }
            .seconds

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

            >**Note:** If email is disabled, there is no way to reset the password.
            
            If there is no account associated with the given email address, 
            a [No Principal Information](https://singularity.stereov.io/docs/guides/auth/security-alerts#no-account-information)
            email will be sent to the given email address.
            
            ### Locale
            
            A locale can be specified for this request. 
            The email will be sent in the specified locale.
            You can learn more about locale in emails [here](https://singularity.stereov.io/docs/guides/email/templates).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            >**Note:** After each email, a cooldown will be started.
            >You can check the status of the cooldown through the endpoint [`GET /api/auth/password/reset/cooldown`](https://singularity.stereov.io/docs/api/get-remaining-password-reset-cooldown).
            >When the cooldown is active, no new email can be sent.
            >The cooldown can be configured [here](https://singularity.stereov.io/docs/guides/email/configuration).
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/authentication#password-reset"),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The number of seconds the user needs to wait to send a new email.",
            )
        ]
    )
    @ThrowsDomainError([
        SendPasswordResetException::class,
    ])
    suspend fun sendPasswordResetEmail(
        @RequestBody @Valid req: SendPasswordResetRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<SendEmailResponse> {
        val remaining = passwordResetService.sendPasswordReset(req, locale)
            .getOrThrow { when (it) { is SendPasswordResetException -> it } }

        return ResponseEntity.ok().body(
            SendEmailResponse(remaining)
        )
    }
}
