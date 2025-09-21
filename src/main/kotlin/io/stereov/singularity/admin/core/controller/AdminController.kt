package io.stereov.singularity.admin.core.controller

import io.stereov.singularity.admin.core.service.AdminService
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.bson.types.ObjectId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admins")
@Tag(name = "Roles", description = "Operations related to roles.")
class AdminController(
    private val adminService: AdminService,
) {

    @PostMapping("{userId}")
    @Operation(
        summary = "Grant Admin Permissions",
        description = """
            Grant [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) permissions to a user.
            
            You can find more information about roles [here](https://singularity.stereov.io/docs/guides/auth/roles).
            
            **Tokens:**
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) permissions is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/roles#admins"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.ADMIN_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.ADMIN_SCOPE]),

        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The updated user information",
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired AccessToken.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "AccessToken does permit [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) access.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun grantAdminPermissions(
        @PathVariable userId: ObjectId
    ): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(adminService.addAdminRole(userId))
    }

    @DeleteMapping("{userId}")
    @Operation(
        summary = "Revoke Admin Permissions",
        description = """
            Grant [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) permissions to a user.
            
            You can find more information about roles [here](https://singularity.stereov.io/docs/guides/auth/roles).
            
            **Tokens:**
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) permissions is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/roles#admins"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.ADMIN_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.ADMIN_SCOPE]),

        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The updated user information",
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired AccessToken.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "AccessToken does permit [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) access.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "Trying to revoke [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) permissions for last remaining admin. At least one admin is required.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun revokeAdminPermissions(
        @PathVariable userId: ObjectId
    ): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(adminService.revokeAdminRole(userId))
    }
}
