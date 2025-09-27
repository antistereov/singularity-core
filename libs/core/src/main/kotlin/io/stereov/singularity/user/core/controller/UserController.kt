package io.stereov.singularity.user.core.controller

import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.global.model.PageableRequest
import io.stereov.singularity.global.model.SuccessResponse
import io.stereov.singularity.global.util.mapContent
import io.stereov.singularity.user.core.dto.response.UserOverviewResponse
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.model.Role
import io.stereov.singularity.user.core.service.UserService
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.bson.types.ObjectId
import org.springframework.data.domain.Page
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
    private val userMapper: UserMapper,
) {

    @GetMapping("/{id}")
    @Operation(
        summary = "Get User By ID",
        description = """
            Get information about a specific user.
            
            You can find more information about user management [here](https://singularity.stereov.io/docs/guides/users/managing-users).
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/users/managing-users"),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The user information",
            ),
            ApiResponse(
                responseCode = "404",
                description = "No user with given ID found.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
        ]
    )
    suspend fun getUserById(
        @PathVariable id: ObjectId
    ): ResponseEntity<UserOverviewResponse> {

        val user = userService.findById(id)

        return ResponseEntity.ok().body(userMapper.toOverview(user))
    }

    @GetMapping
    @Operation(
        summary = "Get Users",
        description = """
            Find all users with the specified filters.
            
            You can find more information about user management [here](https://singularity.stereov.io/docs/guides/users/managing-users).
            
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
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/users/managing-users"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.ADMIN_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.ADMIN_SCOPE]),

        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The users.",
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
    ): ResponseEntity<Page<UserOverviewResponse>> {
        authorizationService.requireRole(Role.ADMIN)

        val users = userService.findAllPaginated(
            pageable = PageableRequest(page, size, sort).toPageable(),
            email = email,
            roles = roles?.map { Role.fromString(it) }?.toSet(),
            groups = groups,
            identityKeys = identities,
            createdAtAfter = createdAtAfter,
            createdAtBefore = createdAtBefore,
            lastActiveAfter = lastActiveAfter,
            lastActiveBefore = lastActiveBefore
        )

        return ResponseEntity.ok()
            .body(users.mapContent { userMapper.toOverview(it) })
    }

    @DeleteMapping("{id}")
    @Operation(
        summary = "Delete User By ID",
        description = """
            Delete a User with given ID.
            
            You can find more information about user management [here](https://singularity.stereov.io/docs/guides/users/managing-users).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) permissions is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/users/managing-users"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.ADMIN_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.ADMIN_SCOPE]),

        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The users.",
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
    suspend fun deleteUserById(
        @PathVariable id: ObjectId
    ): ResponseEntity<SuccessResponse> {

        authorizationService.requireRole(Role.ADMIN)

        if (!userService.existsById(id)) throw DocumentNotFoundException("Deletion failed: no such user exists")
        userService.deleteById(id)

        return ResponseEntity.ok(SuccessResponse())
    }
}
