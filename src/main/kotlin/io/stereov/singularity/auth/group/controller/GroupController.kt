package io.stereov.singularity.auth.group.controller

import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.group.dto.request.CreateGroupRequest
import io.stereov.singularity.auth.group.dto.request.UpdateGroupRequest
import io.stereov.singularity.auth.group.dto.response.GroupResponse
import io.stereov.singularity.auth.group.mapper.GroupMapper
import io.stereov.singularity.auth.group.service.GroupService
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.global.model.SuccessResponse
import io.stereov.singularity.user.core.model.Role
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("api/groups")
@Tag(name = "Groups", description = "Operation related to group and member management.")
class GroupController(
    private val service: GroupService,
    private val groupMapper: GroupMapper,
    private val authorizationService: AuthorizationService
) {

    @PostMapping
    @Operation(
        summary = "Create Group",
        description = """
            Create a new group.
            
            You can find more information about groups [here](https://singularity.stereov.io/docs/guides/auth/groups).
            
            **Locale:**
            
            Group names and descriptions can be specified in multiples languages.
            These can be set when [creating](https://singularity.stereov.io/docs/guides/auth/groups#creating-groups)
            groups or through [updates](https://singularity.stereov.io/docs/guides/auth/groups#updating-groups).
            
            **Note:** A translation for the application's default locale must exist.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            The `locale` request parameter specifies in which language the information should be returned.
            If no locale is specified, the application's default locale will be used.
            
            **Tokens:**
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) permissions is required.
            """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/groups#creating-groups"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.ADMIN_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.ADMIN_SCOPE]),

        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Information about newly created group.",
                content = [Content(schema = Schema(implementation = GroupResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired `AccessToken`.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "User does not have `ADMIN` role.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "Group with this `key` already exists.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun createGroup(
        @RequestBody req: CreateGroupRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<GroupResponse> {
        return ResponseEntity.ok(
            groupMapper.createGroupResponse(service.create(req), locale)
        )
    }

    @GetMapping
    @Operation(
        summary = "Get Groups",
        description = """
            Get all configured groups.
            
            You can find more information about groups [here](https://singularity.stereov.io/docs/guides/auth/groups).
            
            **Locale:**
            
            Group names and descriptions can be specified in multiples languages.
            These can be set when [creating](https://singularity.stereov.io/docs/guides/auth/groups#creating-groups)
            groups or through [updates](https://singularity.stereov.io/docs/guides/auth/groups#updating-groups).
            
            The `locale` request parameter specifies in which language the information should be returned.
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            **Tokens:**
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) permissions is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/groups#getting-groups"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.ADMIN_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.ADMIN_SCOPE]),

        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Paginated groups.",
            ),
            ApiResponse(
                responseCode = "400",
                description = "Translation for default locale is missing.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired `AccessToken`.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "User does not have `ADMIN` permissions.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun getGroups(
        @RequestParam page: Int = 0,
        @RequestParam size: Int = 10,
        @RequestParam sort: List<String> = emptyList(),
        @RequestParam locale: Locale?
    ): ResponseEntity<Page<GroupResponse>> {
        authorizationService.requireRole(Role.ADMIN)
        val pages = service.findAllPaginated(page, size, sort, locale = locale)
        val responses = pages.content.map { group -> groupMapper.createGroupResponse(group, locale) }

        return ResponseEntity.ok(
            PageImpl(responses, pages.pageable, pages.totalElements)
        )
    }

    @GetMapping("/{key}")
    @Operation(
        summary = "Get Group By Key",
        description = """
            Get a group by its `key`.
            
            You can find more information about groups [here](https://singularity.stereov.io/docs/guides/auth/groups).
            
            **Locale:**
            
            Group names and descriptions can be specified in multiples languages.
            These can be set when [creating](https://singularity.stereov.io/docs/guides/auth/groups#creating-groups)
            groups or through [updates](https://singularity.stereov.io/docs/guides/auth/groups#updating-groups).
            
            The `locale` request parameter specifies in which language the information should be returned.
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            **Tokens:**
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) permissions is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/groups#getting-groups"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.ADMIN_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.ADMIN_SCOPE]),

        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Group information.",
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid token.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "`AccessToken` does permit [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles) access.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun getGroupByKey(
        @PathVariable key: String,
        @RequestParam locale: Locale?
    ): ResponseEntity<GroupResponse> {
        authorizationService.requireRole(Role.ADMIN)

        return ResponseEntity.ok(
            groupMapper.createGroupResponse(service.findByKey(key), locale)
        )
    }

    @PutMapping("/{key}")
    @Operation(
        summary = "Update Group",
        description = """
            Update the group with the given `key`.
            
            You can find more information about groups [here](https://singularity.stereov.io/docs/guides/auth/groups).
            
            **Locale:**
            
            Group names and descriptions can be specified in multiples languages.
            These can be set when [creating](https://singularity.stereov.io/docs/guides/auth/groups#creating-groups)
            groups or through [updates](https://singularity.stereov.io/docs/guides/auth/groups#updating-groups).
            
            **Note:** A translation for the application's default locale must exist.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            **Tokens:**
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) permissions is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/groups#updating-groups"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.ADMIN_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.ADMIN_SCOPE]),

        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Updated group information.",
            ),
            ApiResponse(
                responseCode = "400",
                description = "Update would cause the translation for default locale to be missing.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired `AccessToken`.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "`AccessToken` does permit [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles) access.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "A group with this key already exists.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun updateGroup(
        @PathVariable key: String,
        @RequestBody req: UpdateGroupRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<GroupResponse> {
        return ResponseEntity.ok(
            groupMapper.createGroupResponse(service.update(key, req), locale)
        )
    }

    @DeleteMapping("/{key}")
    @Operation(
        summary = "Delete Group",
        description = """
            Delete the group of the given `key`.
            
            You can find more information about groups [here](https://singularity.stereov.io/docs/guides/auth/groups).
            
            **Tokens:**
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) permissions is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/groups#deleting-groups"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.ADMIN_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.ADMIN_SCOPE]),

        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Success.",
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid token.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "AccessToken does permit [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles) access.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "No group with `key` found.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun deleteGroup(
        @PathVariable key: String
    ): ResponseEntity<SuccessResponse> {
        service.deleteByKey(key)
        return ResponseEntity.ok(SuccessResponse())
    }
}
