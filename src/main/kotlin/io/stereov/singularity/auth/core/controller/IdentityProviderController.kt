package io.stereov.singularity.auth.core.controller

import io.stereov.singularity.auth.core.dto.request.ConnectPasswordIdentityRequest
import io.stereov.singularity.auth.core.dto.response.IdentityProviderResponse
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.core.service.IdentityProviderService
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.core.mapper.UserMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth/providers")
@Tag(name = "Identity Provider", description = "Operations related to connecting and disconnecting identity providers to existing accounts.")
class IdentityProviderController(
    private val identityProviderService: IdentityProviderService,
    private val authorizationService: AuthorizationService,
    private val userMapper: UserMapper,
) {

    @GetMapping
    @Operation(
        summary = "Get identity providers",
        description = "Get a list of connected identity providers for the current user.",
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The list of identity providers.",
                content = [Content(array = ArraySchema(schema = Schema(implementation = IdentityProviderResponse::class)))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun getProviders(): ResponseEntity<List<IdentityProviderResponse>> {
        val identityProviders = authorizationService.getCurrentUser().sensitive.identities
            .map { IdentityProviderResponse(it.provider) }

        return ResponseEntity.ok(identityProviders)
    }

    @PostMapping("password")
    @Operation(
        summary = "Add a password identity to the current user",
        description = "Create a new identity provider for the current user that allows the user to login with a password",
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(OpenApiConstants.STEP_UP_HEADER),
            SecurityRequirement(OpenApiConstants.STEP_UP_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Success.",
                content = [Content(schema = Schema(implementation = UserResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad request. User already created a password identity",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun connectPasswordIdentity(@RequestBody req: ConnectPasswordIdentityRequest): ResponseEntity<UserResponse> {
        val user = identityProviderService.connect(req)

        return ResponseEntity.ok(userMapper.toResponse(user))
    }

    @DeleteMapping("{provider}")
    @Operation(
        summary = "Delete an identity provider",
        description = "Delete an identity provider from the connected identity providers of the current user." +
                "You are not allowed to delete the password identity or the only existing identity.",
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(OpenApiConstants.STEP_UP_HEADER),
            SecurityRequirement(OpenApiConstants.STEP_UP_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Success.",
                content = [Content(schema = Schema(implementation = UserResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad request: deleting the password identity or the only registered identity is forbidden.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun deleteProvider(@PathVariable provider: String): ResponseEntity<UserResponse> {
        val user = identityProviderService.disconnect(provider)

        return ResponseEntity.ok(userMapper.toResponse(user))
    }
}
