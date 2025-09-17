package io.stereov.singularity.auth.core.controller

import io.stereov.singularity.auth.core.dto.request.ResetPasswordRequest
import io.stereov.singularity.auth.core.dto.request.SendPasswordResetRequest
import io.stereov.singularity.auth.core.dto.response.MailCooldownResponse
import io.stereov.singularity.auth.core.service.PasswordResetService
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.MailSendResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.global.model.SuccessResponse
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
        description = "Perform a password reset using a PasswordResetToken you obtained from an email. " +
                "If successful, the user can log in using the new password afterwards.",
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
        description = "Get the remaining time in seconds until you can send another password reset request.",
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/authentication#password-reset"),
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
                description = "Unauthorized.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
        ]
    )
    suspend fun getRemainingPasswordResetCooldown(): ResponseEntity<MailCooldownResponse> {
        val remainingCooldown = passwordResetService.getRemainingCooldown()

        return ResponseEntity.ok().body(remainingCooldown)
    }

    @PostMapping("/reset-request")
    @Operation(
        summary = "Send Password Reset Email",
        description = "Send a password reset request email to the user's email. " +
                "After sending an email, a cooldown is triggered " +
                "during which no new passwort reset request email can be sent.",
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
                description = "Failed to send another email. Wait until the cooldown is done.",
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
            MailSendResponse(passwordResetService.getRemainingCooldown().remaining)
        )
    }
}
