package io.stereov.singularity.admin.core.controller

import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.onFailure
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.admin.core.exception.RevokeAdminRoleException
import io.stereov.singularity.admin.core.service.AdminService
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.token.exception.AccessTokenExtractionException
import io.stereov.singularity.database.encryption.exception.SaveEncryptedDocumentException
import io.stereov.singularity.global.annotation.ThrowsDomainError
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.principal.core.dto.response.UserResponse
import io.stereov.singularity.principal.core.exception.FindUserByIdException
import io.stereov.singularity.principal.core.exception.PrincipalMapperException
import io.stereov.singularity.principal.core.mapper.PrincipalMapper
import io.stereov.singularity.principal.core.model.Role
import io.stereov.singularity.principal.core.service.UserService
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
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
    private val accessTokenCache: AccessTokenCache,
    private val authorizationService: AuthorizationService,
    private val userService: UserService,
    private val principalMapper: PrincipalMapper
) {

    private val logger = KotlinLogging.logger {}

    @PostMapping("{userId}")
    @Operation(
        summary = "Grant Admin Permissions",
        description = """
            Grant [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) permissions to a user.
            
            You can find more information about roles [here](https://singularity.stereov.io/docs/guides/auth/roles).
            
            ### Tokens
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
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        AuthenticationException.RoleRequired::class,
        FindUserByIdException::class,
        SaveEncryptedDocumentException::class,
        PrincipalMapperException::class
    ])
    suspend fun grantAdminPermissions(
        @PathVariable userId: ObjectId
    ): ResponseEntity<UserResponse> {
        authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            .requireRole(Role.User.ADMIN)
            .getOrThrow { when (it) { is AuthenticationException.RoleRequired -> it } }

        accessTokenCache.invalidateAllTokens(userId)
            .onFailure { ex -> logger.error(ex) { "Failed to invalidate all tokens for user $userId" } }

        var user = userService.findById(userId)
            .getOrThrow { FindUserByIdException.from(it) }
        user = adminService.addAdminRole(user)
            .getOrThrow { when (it) { is SaveEncryptedDocumentException -> it } }

        val response = principalMapper.toResponse(user)
            .getOrThrow { when (it) { is PrincipalMapperException -> it } }

        return ResponseEntity.ok(response)
    }

    @DeleteMapping("{userId}")
    @Operation(
        summary = "Revoke Admin Permissions",
        description = """
            Grant [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) permissions to a user.
            
            You can find more information about roles [here](https://singularity.stereov.io/docs/guides/auth/roles).
            
            ### Tokens
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
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        AuthenticationException.RoleRequired::class,
        FindUserByIdException::class,
        RevokeAdminRoleException::class,
        PrincipalMapperException::class
    ])
    suspend fun revokeAdminPermissions(
        @PathVariable userId: ObjectId
    ): ResponseEntity<UserResponse> {
        authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            .requireRole(Role.User.ADMIN)
            .getOrThrow { when (it) { is AuthenticationException.RoleRequired -> it } }

        accessTokenCache.invalidateAllTokens(userId)
            .onFailure { ex -> logger.error(ex) { "Failed to invalidate all tokens for user $userId" } }

        var user = userService.findById(userId)
            .getOrThrow { FindUserByIdException.from(it) }

        accessTokenCache.invalidateAllTokens(userId)
            .onFailure { ex -> logger.error(ex) { "Failed to invalidate all tokens for user $userId" } }

        user = adminService.revokeAdminRole(user)
            .getOrThrow { when (it) { is RevokeAdminRoleException -> it }}

        val response = principalMapper.toResponse(user)
            .getOrThrow { when (it) { is PrincipalMapperException -> it } }

        return ResponseEntity.ok(response)
    }
}
