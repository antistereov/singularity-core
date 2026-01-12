package io.stereov.singularity.principal.group.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.token.exception.AccessTokenExtractionException
import io.stereov.singularity.database.core.exception.DatabaseException
import io.stereov.singularity.database.core.exception.FindAllDocumentsPaginatedException
import io.stereov.singularity.database.core.exception.FindDocumentByKeyException
import io.stereov.singularity.global.annotation.ThrowsDomainError
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.global.model.SuccessResponse
import io.stereov.singularity.principal.core.model.Role
import io.stereov.singularity.principal.group.dto.request.CreateGroupRequest
import io.stereov.singularity.principal.group.dto.request.UpdateGroupRequest
import io.stereov.singularity.principal.group.dto.response.GroupResponse
import io.stereov.singularity.principal.group.exception.CreateGroupException
import io.stereov.singularity.principal.group.exception.DeleteGroupByKeyException
import io.stereov.singularity.principal.group.exception.UpdateGroupException
import io.stereov.singularity.principal.group.mapper.GroupMapper
import io.stereov.singularity.principal.group.service.GroupService
import io.stereov.singularity.translate.exception.TranslateException
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
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
            
            ### Locale
            
            Group names and descriptions can be specified in multiples languages.
            These can be set when [creating](https://singularity.stereov.io/docs/guides/auth/groups#creating-groups)
            groups or through [updates](https://singularity.stereov.io/docs/guides/auth/groups#updating-groups).
            
            >**Note:** A translation for the application's default locale must exist.
            >You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            The `locale` in the request body specifies which translation should be created.
            The `locale` query parameter specifies which translation should be returned.
            
            ### Tokens
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
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        AuthenticationException.RoleRequired::class,
        CreateGroupException::class,
        TranslateException.NoTranslations::class,
    ])
    suspend fun createGroup(
        @RequestBody req: CreateGroupRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<GroupResponse> {
        authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            .requireRole(Role.User.ADMIN)
            .getOrThrow { when (it) { is AuthenticationException.RoleRequired -> it } }

        val group = service.create(req)
            .getOrThrow { when (it) { is CreateGroupException -> it } }
        val response = groupMapper.createGroupResponse(group, locale)
            .getOrThrow { when (it) { is TranslateException.NoTranslations -> it } }

        return ResponseEntity.ok(response)
    }

    @GetMapping
    @Operation(
        summary = "Get Groups",
        description = """
            Get all configured groups.
            
            You can find more information about groups [here](https://singularity.stereov.io/docs/guides/auth/groups).
            
            ### Locale
            
            Group names and descriptions can be specified in multiples languages.
            These can be set when [creating](https://singularity.stereov.io/docs/guides/auth/groups#creating-groups)
            groups or through [updates](https://singularity.stereov.io/docs/guides/auth/groups#updating-groups).
            
            The `locale` request parameter specifies in which language the information should be returned.
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            ### Tokens
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
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        AuthenticationException.RoleRequired::class,
        DatabaseException.Database::class,
        TranslateException.NoTranslations::class,
    ])
    suspend fun getGroups(
        pageable: Pageable,
        @RequestParam locale: Locale?
    ): ResponseEntity<PagedModel<GroupResponse>> {
        authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            .requireRole(Role.User.ADMIN)
            .getOrThrow { when (it) { is AuthenticationException.RoleRequired -> it } }

        val pages = service.findAllPaginated(pageable = pageable, locale = locale)
            .getOrThrow { when (it) { is FindAllDocumentsPaginatedException -> it } }
        val responses = pages.content.map { group ->
            groupMapper.createGroupResponse(group, locale)
                .getOrThrow { when (it) { is TranslateException.NoTranslations -> it } }
        }

        return ResponseEntity.ok(
            PagedModel(PageImpl(responses, pages.pageable, pages.totalElements))
        )
    }

    @GetMapping("/{key}")
    @Operation(
        summary = "Get Group By Key",
        description = """
            Get a group by its `key`.
            
            You can find more information about groups [here](https://singularity.stereov.io/docs/guides/auth/groups).
            
            ### Locale
            
            Group names and descriptions can be specified in multiples languages.
            These can be set when [creating](https://singularity.stereov.io/docs/guides/auth/groups#creating-groups)
            groups or through [updates](https://singularity.stereov.io/docs/guides/auth/groups#updating-groups).
            
            The `locale` request parameter specifies in which language the information should be returned.
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            ### Tokens
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
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        AuthenticationException.RoleRequired::class,
        FindDocumentByKeyException::class,
        TranslateException.NoTranslations::class,
    ])
    suspend fun getGroupByKey(
        @PathVariable key: String,
        @RequestParam locale: Locale?
    ): ResponseEntity<GroupResponse> {
        authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            .requireRole(Role.User.ADMIN)
            .getOrThrow { when (it) { is AuthenticationException.RoleRequired -> it } }

        val group = service.findByKey(key)
            .getOrThrow { when (it) { is FindDocumentByKeyException -> it } }
        val response = groupMapper.createGroupResponse(group, locale)
            .getOrThrow { when (it) { is TranslateException.NoTranslations -> it } }

        return ResponseEntity.ok(response)
    }

    @PutMapping("/{key}")
    @Operation(
        summary = "Update Group",
        description = """
            Update the group with the given `key`.
            
            You can find more information about groups [here](https://singularity.stereov.io/docs/guides/auth/groups).
            
            ### Locale
            
            Group names and descriptions can be specified in multiples languages.
            These can be set when [creating](https://singularity.stereov.io/docs/guides/auth/groups#creating-groups)
            groups or through [updates](https://singularity.stereov.io/docs/guides/auth/groups#updating-groups).
            
            >**Note:** A translation for the application's default locale must exist.
            >You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            The `locale` in the request body specifies which translation should be created.
            The `locale` query parameter specifies which translation should be returned.
            
            ### Tokens
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
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        AuthenticationException.RoleRequired::class,
        UpdateGroupException::class,
        TranslateException.NoTranslations::class,
    ])
    suspend fun updateGroup(
        @PathVariable key: String,
        @RequestBody req: UpdateGroupRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<GroupResponse> {
        authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            .requireRole(Role.User.ADMIN)
            .getOrThrow { when (it) { is AuthenticationException.RoleRequired -> it } }

        val group = service.update(key, req)
            .getOrThrow { when (it) { is UpdateGroupException -> it } }
        val response = groupMapper.createGroupResponse(group, locale)
            .getOrThrow { when (it) { is TranslateException.NoTranslations -> it } }
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{key}")
    @Operation(
        summary = "Delete Group",
        description = """
            Delete the group of the given `key`.
            
            You can find more information about groups [here](https://singularity.stereov.io/docs/guides/auth/groups).
            
            ### Tokens
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
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        AuthenticationException.RoleRequired::class,
        DeleteGroupByKeyException::class,
    ])
    suspend fun deleteGroup(
        @PathVariable key: String
    ): ResponseEntity<SuccessResponse> {
        authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            .requireRole(Role.User.ADMIN)
            .getOrThrow { when (it) { is AuthenticationException.RoleRequired -> it } }

        service.deleteByKeyAndUpdateMembers(key)
            .getOrThrow { when (it) { is DeleteGroupByKeyException -> it } }
        return ResponseEntity.ok(SuccessResponse())
    }
}
