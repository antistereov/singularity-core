package io.stereov.singularity.principal.core.controller

import com.github.michaelbull.result.get
import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.token.exception.AccessTokenExtractionException
import io.stereov.singularity.database.encryption.exception.DeleteEncryptedDocumentByIdException
import io.stereov.singularity.file.core.exception.FileException
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.global.annotation.ThrowsDomainError
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.global.model.PageableRequest
import io.stereov.singularity.global.model.SuccessResponse
import io.stereov.singularity.global.util.mapContent
import io.stereov.singularity.principal.core.dto.response.PrincipalOverviewResponse
import io.stereov.singularity.principal.core.exception.FindUserByIdException
import io.stereov.singularity.principal.core.exception.GetUsersException
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
import org.springframework.data.web.PagedModel
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/users")
@Tag(
    name = "Managing Users",
    description = "Operations related to user management."
)
class UserController(
    private val userService: UserService,
    private val authorizationService: AuthorizationService,
    private val principalMapper: PrincipalMapper,
    private val fileStorage: FileStorage,
) {

    @GetMapping("/{id}")
    @Operation(
        summary = "Get User By ID",
        description = """
            Get information about a specific user.
            
            You can find more information about user management [here](https://singularity.stereov.io/docs/guides/principals/managing-users).
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/principals/managing-users"),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The user information",
            )
        ]
    )
    @ThrowsDomainError([
        FindUserByIdException::class,
        AccessTokenExtractionException::class,
        PrincipalMapperException::class
    ])
    suspend fun getUserById(
        @PathVariable id: ObjectId
    ): ResponseEntity<PrincipalOverviewResponse> {

        val user = userService.findById(id)
            .getOrThrow { FindUserByIdException.from(it) }

        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) {
                is AccessTokenExtractionException -> it
            } }

        val userOverview = principalMapper.toOverview(user, authenticationOutcome)
            .getOrThrow { when (it) {
                is PrincipalMapperException -> it
            } }

        return ResponseEntity.ok().body(userOverview)
    }

    @GetMapping
    @Operation(
        summary = "Get Users",
        description = """
            Find all users with the specified filters.
            
            You can find more information about user management [here](https://singularity.stereov.io/docs/guides/principals/managing-users).
            
            **Filter:**
            
            * **`email`**: The email of a user; must be an exact match.
            * **`roles`**: A set of roles the users should have.
            * **`groups`**: A set of groups the users should be a member of.
            * **`identities`**: A set of identity providers the users should be connected to.
            * **`createdAtAfter`:** A date the users should be created after.
            * **`createdAtBefore`:** A date the users should be created before.
            * **`lastActiveAfter`:** A date the users should be last active after.
            * **`lastActiveBefore`:** A date the users should be last active before.
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) permissions is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/principals/managing-users"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.ADMIN_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.ADMIN_SCOPE]),

        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The users.",
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        AuthenticationException.RoleRequired::class,
        GetUsersException::class,
        PrincipalMapperException::class
    ])
    suspend fun getUsers(
        @RequestParam page: Int = 0,
        @RequestParam size: Int = 10,
        @RequestParam sort: List<String> = emptyList(),
        @RequestParam(required = false) email: String?,
        @RequestParam(required = false) roles: Set<String>?,
        @RequestParam(required = false) groups: Set<String>?,
        @RequestParam(required = false) identities: Set<String>?,
        @RequestParam(required = false) createdAtAfter: Instant?,
        @RequestParam(required = false) createdAtBefore: Instant?,
        @RequestParam(required = false) lastActiveAfter: Instant?,
        @RequestParam(required = false) lastActiveBefore: Instant?
    ): ResponseEntity<PagedModel<PrincipalOverviewResponse>> {
        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            .requireRole(Role.User.ADMIN)
            .getOrThrow { when (it) { is AuthenticationException.RoleRequired -> it } }

        val mappedRoles = roles?.mapNotNull { Role.fromString(it).get() }?.toSet()

        val users = userService.findAllPaginated(
            pageable = PageableRequest(page, size, sort).toPageable(),
            email = email,
            roles = mappedRoles,
            groups = groups,
            identityKeys = identities,
            createdAtAfter = createdAtAfter,
            createdAtBefore = createdAtBefore,
            lastActiveAfter = lastActiveAfter,
            lastActiveBefore = lastActiveBefore
        ).getOrThrow { when (it) { is GetUsersException -> it } }

        val mappedUsers = users.mapContent {
            principalMapper.toOverview(it, authenticationOutcome)
                .getOrThrow { ex -> when (ex) { is PrincipalMapperException -> ex } }
        }

        return ResponseEntity.ok().body(PagedModel(mappedUsers))
    }

    @DeleteMapping("{id}")
    @Operation(
        summary = "Delete User By ID",
        description = """
            Delete a User with given ID.
            
            You can find more information about user management [here](https://singularity.stereov.io/docs/guides/principals/managing-users).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) permissions is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/principals/managing-users"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.ADMIN_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.ADMIN_SCOPE]),

        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The users.",
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        AuthenticationException.RoleRequired::class,
        FindUserByIdException::class,
        FileException::class,
        DeleteEncryptedDocumentByIdException::class,
    ])
    suspend fun deleteUserById(
        @PathVariable id: ObjectId
    ): ResponseEntity<SuccessResponse> {
        authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            .requireRole(Role.User.ADMIN)
            .getOrThrow { when (it) { is AuthenticationException.RoleRequired -> it } }

        val user = userService.findById(id)
            .getOrThrow { FindUserByIdException.from(it) }

        user.sensitive.avatarFileKey?.let {
            fileStorage.remove(it).getOrThrow { ex -> when (ex) { is FileException -> ex } }
        }
        userService.deleteById(id)
            .getOrThrow { when (it) { is DeleteEncryptedDocumentByIdException -> it } }

        return ResponseEntity.ok(SuccessResponse())
    }
}
