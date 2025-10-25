package io.stereov.singularity.auth.oauth2.controller

import io.stereov.singularity.auth.core.dto.response.IdentityProviderResponse
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.oauth2.dto.request.AddPasswordAuthenticationRequest
import io.stereov.singularity.auth.oauth2.service.IdentityProviderService
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
import jakarta.validation.Valid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/users")
@ConditionalOnProperty("singularity.auth.oauth2.enable", matchIfMissing = false)
@Tag(name = "OAuth2", description = "Operations related to connecting and disconnecting identity providers to existing accounts.")
class IdentityProviderController(
    private val identityProviderService: IdentityProviderService,
    private val authorizationService: AuthorizationService,
    private val userMapper: UserMapper,
) {

    @GetMapping("me/providers")
    @Operation(
        summary = "Get Identity Providers",
        description = """
            Get a list of connected identity providers for the current user.
            
            Users can connect multiple OAuth2 providers to their account.
            You can learn more about this [here](https://singularity.stereov.io/docs/guides/auth/oauth2).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/oauth2#getting-connected-providers"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The list of identity providers.",
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid `AccessToken`.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun getIdentityProviders(): ResponseEntity<List<IdentityProviderResponse>> {
        val identityProviders = authorizationService.getUser().sensitive.identities
            .map { IdentityProviderResponse(it.key) }

        return ResponseEntity.ok(identityProviders)
    }

    @PostMapping("me/providers/password")
    @Operation(
        summary = "Add Password Authentication",
        description = """
            Create a new identity provider for the current user that allows the user to login with a password.
            
            You can learn more about this [here](https://singularity.stereov.io/docs/guides/auth/oauth2#adding-password-authentication).
            
            ### Requirements
            - The user registered via an OAuth2 provider.
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
            - A valid [`StepUpToken`](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)
              is required. This token should match user and session contained in the `AccessToken`.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/oauth2#adding-password-authentication"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(OpenApiConstants.STEP_UP_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.STEP_UP_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Success.",
                content = [Content(schema = Schema(implementation = UserResponse::class))]
            ),
            ApiResponse(
                responseCode = "304",
                description = "User already created a password identity.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Guests are not allowed to add a password identity this way. They need to be converted to users.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid `AccessToken` or `StepUpToken`.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
        ]
    )
    suspend fun addPasswordAuthentication(@RequestBody @Valid req: AddPasswordAuthenticationRequest): ResponseEntity<UserResponse> {
        val user = identityProviderService.connect(req)

        return ResponseEntity.ok(userMapper.toResponse(user))
    }

    @DeleteMapping("me/providers/{provider}")
    @Operation(
        summary = "Delete Identity Provider",
        description = """
            Delete an identity provider from the connected identity providers of the current user.
            
            ### Requirements
            - You are not allowed to delete the password identity or the only existing identity.
            
            A [security alert](https://singularity.stereov.io/docs/guides/auth/security-alerts#oauth2-specific-alerts)
            will be sent to the user's email if this setting is enabled and
            email is [enabled and configured correctly](https://singularity.stereov.io/docs/guides/email/configuration).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
            - A valid [`StepUpToken`](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)
              is required. This token should match user and session contained in the `AccessToken`.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/oauth2#disconnecting-an-oauth2-provider"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(OpenApiConstants.STEP_UP_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.STEP_UP_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Success.",
                content = [Content(schema = Schema(implementation = UserResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Deleting the password identity or the only registered identity is forbidden.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid `AccessToken` or `StepUpToken`.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "The requested provider is not connected.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
        ]
    )
    suspend fun deleteIdentityProvider(
        @PathVariable provider: String,
        @RequestParam locale: Locale?
    ): ResponseEntity<UserResponse> {
        val user = identityProviderService.disconnect(provider, locale)

        return ResponseEntity.ok(userMapper.toResponse(user))
    }
}
