package io.stereov.singularity.user.settings.controller

import io.stereov.singularity.auth.core.component.CookieCreator
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.settings.dto.request.ChangeEmailRequest
import io.stereov.singularity.user.settings.dto.request.ChangePasswordRequest
import io.stereov.singularity.user.settings.dto.request.ChangeUserRequest
import io.stereov.singularity.user.settings.service.UserSettingsService
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/users/me")
@Tag(
    name = "Profile Management",
    description = "Operations related to user settings."
)
class UserSettingsController(
    private val userMapper: UserMapper,
    private val userSettingsService: UserSettingsService,
    private val cookieCreator: CookieCreator,
    private val authorizationService: AuthorizationService
) {

    @GetMapping
    @Operation(
        summary = "Get User",
        description = """
            Retrieves the user profile information of the currently authenticated user.
            
            You can find more information about profile management [here](https://singularity.stereov.io/docs/guides/users/profile-management).
            
            **Tokens:**
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/users/profile-management"),
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "User information.",
            ),
            ApiResponse(
                responseCode = "401",
                description = "`AccessToken` is invalid or expired.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun getAuthorizedUser(): ResponseEntity<UserResponse> {
        val user = authorizationService.getUser()

        return ResponseEntity.ok(userMapper.toResponse(user))
    }

    @PutMapping("/email")
    @Operation(
        summary = "Change Email",
        description = """
            Change the email of the currently authenticated user.
            
            If [email is enabled](https://singularity.stereov.io/docs/guides/email/configuration),
            the user needs to verify this email address with a token that is sent to the new email address.
            Only then the email is changed in the database.
            If email is disabled, the email will be changed immediately.
            
            **Requirements:**
            - The `email` should be a valid email address (e.g., "test@example.com")
              that is not associated to an existing account.
            
            You can find more information about profile management [here](https://singularity.stereov.io/docs/guides/users/profile-management).
            
            **Tokens:**
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
            - A valid [`StepUpToken`](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)
              is required. This token should match user and session contained in the `AccessToken`.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/users/profile-management"),
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "User information.",
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid `email` provided",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "`AccessToken` or `StepUpToken` are invalid or expired.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun changeEmailOfAuthorizedUser(
        @RequestBody @Valid payload: ChangeEmailRequest,
        @RequestParam locale: Locale?,
    ): ResponseEntity<UserResponse> {
        val user = userSettingsService.changeEmail(payload, locale)
        return ResponseEntity.ok().body(
            userMapper.toResponse(user)
        )
    }

    @Operation(
        summary = "Change Password",
        description = """
            Change the password of the currently authenticated user.
            
            You can find more information about profile management [here](https://singularity.stereov.io/docs/guides/users/profile-management).
            
            **Requirements:**
            - The `password` must be at least 8 characters long and include at least one uppercase letter, 
              one lowercase letter, one number, and one special character (!@#$%^&*()_+={}[]|\:;'"<>,.?/).
            
            **Tokens:**
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
            - A valid [`StepUpToken`](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)
              is required. This token should match user and session contained in the `AccessToken`.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/users/profile-management"),
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "User information.",
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid `password` provided",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "`AccessToken` or `StepUpToken` are invalid or expired or password is wrong.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PutMapping("/password")
    suspend fun changePasswordOfAuthorizedUser(
        @RequestBody @Valid payload: ChangePasswordRequest
    ): ResponseEntity<UserResponse> {
        val user = userSettingsService.changePassword(payload)

        return ResponseEntity.ok().body(
            userMapper.toResponse(user)
        )
    }

    @PutMapping
    @Operation(
        summary = "Update User",
        description = """
            Update the user information of the currently authenticated user.
            
            You can find more information about profile management [here](https://singularity.stereov.io/docs/guides/users/profile-management).
            
            **Tokens:**
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/users/profile-management"),
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "User information.",
            ),
            ApiResponse(
                responseCode = "401",
                description = "`AccessToken` is invalid or expired.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun updateAuthorizedUser(@RequestBody payload: ChangeUserRequest): ResponseEntity<UserResponse> {
        val user = userSettingsService.changeUser(payload)

        return ResponseEntity.ok().body(
            userMapper.toResponse(user)
        )
    }

    @PutMapping("/avatar")
    @Operation(
        summary = "Update Avatar",
        description = """
            Update the avatar of the currently authenticated user.
            
            You can find more information about profile management [here](https://singularity.stereov.io/docs/guides/users/profile-management).
            
            **Tokens:**
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/users/profile-management"),
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "User information.",
            ),
            ApiResponse(
                responseCode = "401",
                description = "`AccessToken` is invalid or expired.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun setAvatarOfAuthorizedUser(
        @RequestPart file: FilePart
    ): ResponseEntity<UserResponse> {
        return ResponseEntity.ok().body(
            userSettingsService.setAvatar(file)
        )
    }

    @DeleteMapping("/avatar")
    @Operation(
        summary = "Delete Avatar",
        description = """
            Delete the avatar of the currently authenticated user.
            
            You can find more information about profile management [here](https://singularity.stereov.io/docs/guides/users/profile-management).
            
            **Tokens:**
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/users/profile-management"),
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "User information.",
            ),
            ApiResponse(
                responseCode = "401",
                description = "`AccessToken` is invalid or expired.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun deleteAvatarOfAuthorizedUser(): ResponseEntity<UserResponse> {
        return ResponseEntity.ok().body(
            userSettingsService.deleteAvatar()
        )
    }

    @DeleteMapping
    @Operation(
        summary = "Delete User",
        description = """
            Delete the currently authenticated user.
            
            You can find more information about profile management [here](https://singularity.stereov.io/docs/guides/users/profile-management).
            
            **Tokens:**
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
            - A valid [`StepUpToken`](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)
              is required. This token should match user and session contained in the `AccessToken`.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/users/profile-management"),
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "User information.",
            ),
            ApiResponse(
                responseCode = "401",
                description = "`AccessToken` or `StepUpToken` are invalid or expired.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun deleteAuthorizedUser(): ResponseEntity<Map<String, String>> {
        val clearAccessTokenCookie = cookieCreator.clearCookie(SessionTokenType.Access)
        val clearRefreshTokenCookie = cookieCreator.clearCookie(SessionTokenType.Refresh)
        val clearStepUpTokenCookie = cookieCreator.clearCookie(SessionTokenType.StepUp)

        userSettingsService.deleteUser()

        return ResponseEntity.ok()
            .header("Set-Cookie", clearAccessTokenCookie.value)
            .header("Set-Cookie", clearRefreshTokenCookie.value)
            .header("Set-Cookie", clearStepUpTokenCookie.value)
            .body(mapOf("message" to "success"))
    }
}
