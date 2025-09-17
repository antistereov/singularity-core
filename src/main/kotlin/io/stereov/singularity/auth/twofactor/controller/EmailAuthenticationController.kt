package io.stereov.singularity.auth.twofactor.controller

import io.stereov.singularity.auth.core.dto.response.MailCooldownResponse
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.twofactor.dto.request.EnableEmailTwoFactorMethodRequest
import io.stereov.singularity.auth.twofactor.service.EmailAuthenticationService
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.MailSendResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.global.model.SuccessResponse
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
import java.util.*

@RestController
@RequestMapping("/api/auth/2fa/email")
@Tag(
    name = "Two-Factor Authentication"
)
class EmailAuthenticationController(
    private val emailAuthenticationService: EmailAuthenticationService,
    private val authorizationService: AuthorizationService,
    private val userMapper: UserMapper
) {

    @PostMapping("/send")
    @Operation(
        summary = "Send Email 2FA Code",
        description = "Send an email containing a 2FA code to the user. " +
                "It will update the code in the database. " +
                "Needs to be called before enabling this method for the user.",
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/two-factor#sending-a-2fa-code-via-email"),
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE),
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Success.",
                content = [Content(schema = Schema(implementation = SuccessResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Not authorized.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun sendEmailTwoFactorCode(
        @RequestParam locale: Locale?
    ): ResponseEntity<MailSendResponse> {
        val user = authorizationService.getCurrentUser()

        emailAuthenticationService.sendMail(user, locale)

        return ResponseEntity.ok(MailSendResponse(emailAuthenticationService.getRemainingCooldown(user.id)))
    }

    @PostMapping("/enable")
    @Operation(
        summary = "Enable Email as 2FA Method",
        description = "Enable email as 2FA method. Step-up is required.",
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
                responseCode = "401",
                description = "Not authorized.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "This method is already enabled for the user.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun enableEmailAsTwoFactorMethod(
        @RequestBody payload: EnableEmailTwoFactorMethodRequest
    ): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(
            userMapper.toResponse(emailAuthenticationService.enable(payload))
        )
    }

    @DeleteMapping
    @Operation(
        summary = "Disable Email as 2FA Method",
        description = "Disable email as 2FA method. Step-up is required.",
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
                responseCode = "401",
                description = "Not authorized.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "This method is already enabled for the user.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun disableEmailAsTwoFactorMethod(): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(
            userMapper.toResponse(emailAuthenticationService.disable())
        )
    }

    @GetMapping("/cooldown")
    @Operation(
        summary = "Get Remaining Email 2FA Code Cooldown",
        description = "Get the remaining time in seconds until you can send another email containing a 2FA code.",
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/two-factor#sending-a-2fa-code-via-email"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The remaining cooldown.",
                content = [Content(schema = Schema(implementation = MailCooldownResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
        ]
    )
    suspend fun getRemainingEmailTwoFactorCooldown(): ResponseEntity<MailCooldownResponse> {
        val userId = authorizationService.getCurrentUserId()
        val remainingCooldown = emailAuthenticationService.getRemainingCooldown(userId)

        return ResponseEntity.ok().body(MailCooldownResponse(remainingCooldown))
    }
}
