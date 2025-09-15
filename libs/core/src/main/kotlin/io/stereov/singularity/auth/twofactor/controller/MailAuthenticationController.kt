package io.stereov.singularity.auth.twofactor.controller

import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.twofactor.dto.request.EnableMailTwoFactorMethodRequest
import io.stereov.singularity.auth.twofactor.service.MailAuthenticationService
import io.stereov.singularity.content.translate.model.Language
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.global.model.SuccessResponse
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

@RestController
@RequestMapping("/api/auth/2fa/mail")
@Tag(
    name = "Mail Two-Factor Authentication",
    description = "Operations related to 2FA using mail"
)
class MailAuthenticationController(
    private val mailAuthenticationService: MailAuthenticationService,
    private val authorizationService: AuthorizationService,
    private val userMapper: UserMapper
) {

    @PostMapping("/send")
    @Operation(
        summary = "Send a 2FA mail code",
        description = "Send an email containing a 2FA code to the user. " +
                "It will update the code in the database. " +
                "Needs to be called before enabling this method for the user.",
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
    suspend fun sendAuthenticationMail(
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<SuccessResponse> {
        val user = authorizationService.getCurrentUser()

        mailAuthenticationService.sendMail(user, lang)

        return ResponseEntity.ok(SuccessResponse())
    }

    @PostMapping("/enable")
    @Operation(
        summary = "Enable mail as 2FA method",
        description = "Enable mail as 2FA method. Step-up is required.",
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
    suspend fun enableMail(
        @RequestBody payload: EnableMailTwoFactorMethodRequest
    ): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(
            userMapper.toResponse(mailAuthenticationService.enable(payload))
        )
    }

    @DeleteMapping
    @Operation(
        summary = "Disable mail as 2FA method",
        description = "Disable mail as 2FA method. Step-up is required.",
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
    suspend fun disableMail(): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(
            userMapper.toResponse(mailAuthenticationService.disable())
        )
    }
}