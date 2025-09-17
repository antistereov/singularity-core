package io.stereov.singularity.auth.core.controller

import io.stereov.singularity.auth.core.dto.response.MailCooldownResponse
import io.stereov.singularity.auth.core.service.EmailVerificationService
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.MailSendResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.user.core.dto.response.UserResponse
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
@RequestMapping("/api/auth/email/verify")
@Tag(
    name = "Authentication",
)
class EmailVerificationController(
    private val emailVerificationService: EmailVerificationService,
) {

    @PostMapping
    @Operation(
        summary = "Verify Email",
        description = "Verify the user's email by validating the token.",
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/docs/auth/authentication#email-verification"),
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
        ]
    )
    suspend fun verifyEmail(@RequestParam token: String): ResponseEntity<UserResponse> {
        val authInfo = emailVerificationService.verifyEmail(token)

        return ResponseEntity.ok()
            .body(authInfo)
    }

    @GetMapping("/cooldown")
    @Operation(
        summary = "Get Remaining Email Verification Cooldown",
        description = "Get the remaining time in seconds until you can send another verification request.",
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
                description = "Unauthorized.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
        ]
    )
    suspend fun getRemainingEmailVerificationCooldown(): ResponseEntity<MailCooldownResponse> {
        val remainingCooldown = emailVerificationService.getRemainingCooldown()

        return ResponseEntity.ok().body(remainingCooldown)
    }

    @PostMapping("/send")
    @Operation(
        summary = "Send Email Verification Email",
        description = "Send a new verification email to the user's email. " +
                "After sending an email, a cooldown is triggered " +
                "during which no new verification email can be sent.",
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
    suspend fun sendEmailVerificationEmail(
        @RequestParam locale: Locale?
    ): ResponseEntity<MailSendResponse> {

        emailVerificationService.sendEmailVerificationToken(locale)

        return ResponseEntity.ok().body(
            MailSendResponse(emailVerificationService.getRemainingCooldown().remaining)
        )
    }
}
