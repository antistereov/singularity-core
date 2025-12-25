package io.stereov.singularity.principal.group.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.token.exception.AccessTokenExtractionException
import io.stereov.singularity.global.annotation.ThrowsDomainError
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.principal.core.dto.response.PrincipalResponse
import io.stereov.singularity.principal.core.exception.PrincipalMapperException
import io.stereov.singularity.principal.core.mapper.PrincipalMapper
import io.stereov.singularity.principal.core.model.Role
import io.stereov.singularity.principal.group.exception.GroupMemberException
import io.stereov.singularity.principal.group.service.GroupMemberService
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.bson.types.ObjectId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/groups")
@Tag(name = "Groups")
class GroupMemberController(
    private val groupMemberService: GroupMemberService,
    private val principalMapper: PrincipalMapper,
    private val authorizationService: AuthorizationService
) {

    @PostMapping("/{groupKey}/members/{userId}")
    @Operation(
        summary = "Add Member to Group",
        description = """
            Add a member to a group with given `key`. 
            
            You can find more information about groups [here](https://singularity.stereov.io/docs/guides/auth/groups).
            
            >**Note:** Invalidates all [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
            >because they would contain the wrong group information.
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) permissions is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/groups#adding-members-to-groups"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.ADMIN_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.ADMIN_SCOPE]),

        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Updated user information.",
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        AuthenticationException.RoleRequired::class,
        GroupMemberException::class,
        PrincipalMapperException::class,
    ])
    suspend fun addMemberToGroup(
        @PathVariable groupKey: String,
        @PathVariable userId: ObjectId,
    ): ResponseEntity<PrincipalResponse> {
        val authentication = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            .requireRole(Role.User.ADMIN)
            .getOrThrow { when (it) { is AuthenticationException.RoleRequired -> it } }

        val user = groupMemberService.add(userId, groupKey)
            .getOrThrow { when (it) { is GroupMemberException -> it } }

        val response = principalMapper.toResponse(user, authentication)
            .getOrThrow { when (it) { is PrincipalMapperException -> it } }

        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{groupKey}/members/{userId}")
    @Operation(
        summary = "Remove Member from Group",
        description = """
            Remove a member from the group with given `key`.
            
            You can find more information about groups [here](https://singularity.stereov.io/docs/guides/auth/groups).
            
            >**Note:** Invalidates all [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
            >because they would contain the wrong group information.
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) permissions is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/groups#removing-members-from-groups"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.ADMIN_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.ADMIN_SCOPE]),

        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Updated user information.",
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        AuthenticationException.RoleRequired::class,
        GroupMemberException::class,
        PrincipalMapperException::class,
    ])
    suspend fun removeMemberFromGroup(
        @PathVariable groupKey: String,
        @PathVariable userId: ObjectId
    ): ResponseEntity<PrincipalResponse> {
        val authentication = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            .requireRole(Role.User.ADMIN)
            .getOrThrow { when (it) { is AuthenticationException.RoleRequired -> it } }

        val user = groupMemberService.remove(userId, groupKey)
            .getOrThrow { when (it) { is GroupMemberException -> it } }

        val response = principalMapper.toResponse(user, authentication)
            .getOrThrow { when (it) { is PrincipalMapperException -> it } }

        return ResponseEntity.ok(response)
    }
}
