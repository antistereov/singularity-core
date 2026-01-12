package io.stereov.singularity.content.tag.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.token.exception.AccessTokenExtractionException
import io.stereov.singularity.content.tag.dto.CreateTagRequest
import io.stereov.singularity.content.tag.dto.TagResponse
import io.stereov.singularity.content.tag.dto.UpdateTagRequest
import io.stereov.singularity.content.tag.exception.CreateTagException
import io.stereov.singularity.content.tag.exception.UpdateTagException
import io.stereov.singularity.content.tag.mapper.TagMapper
import io.stereov.singularity.content.tag.service.TagService
import io.stereov.singularity.database.core.exception.DeleteDocumentByKeyException
import io.stereov.singularity.database.core.exception.FindAllDocumentsPaginatedException
import io.stereov.singularity.database.core.exception.FindDocumentByKeyException
import io.stereov.singularity.global.annotation.ThrowsDomainError
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.global.model.PageableRequest
import io.stereov.singularity.global.model.SuccessResponse
import io.stereov.singularity.global.util.mapContent
import io.stereov.singularity.principal.group.model.KnownGroups
import io.stereov.singularity.translate.exception.TranslateException
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.web.PagedModel
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/content/tags")
@Tag(name = "Tags", description = "Operations related to tags.")
class TagController(
    private val service: TagService,
    private val tagMapper: TagMapper,
    private val authorizationService: AuthorizationService
) {

    @PostMapping
    @Operation(
        summary = "Create Tag",
        description = """
            Create a new tag.
            
            You can find more information about tags [here](https://singularity.stereov.io/docs/guides/content/tags).
            
            ### Locale
            The `locale` in the request body specifies which translation should be created.
            The `locale` query parameter specifies which translation should be returned.
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).

            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              from a member of the [`CONTRIBUTOR`](https://singularity.stereov.io/docs/guides/content/introduction#global-server-group-contributor) group.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/tags"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.CONTRIBUTOR_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.CONTRIBUTOR_SCOPE]),
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The newly created tag.",
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        AuthenticationException.GroupMembershipRequired::class,
        CreateTagException::class,
        TranslateException.NoTranslations::class
    ])
    suspend fun createTag(@RequestBody req: CreateTagRequest): ResponseEntity<TagResponse> {
        authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            .requireGroupMembership(KnownGroups.CONTRIBUTOR)
            .getOrThrow { when (it) { is AuthenticationException.GroupMembershipRequired -> it } }

        val tag = service.create(req)
            .getOrThrow { when (it) { is CreateTagException -> it } }
        val response = tagMapper.createTagResponse(tag, req.locale)
            .getOrThrow { when (it) { is TranslateException.NoTranslations -> it } }

        return ResponseEntity.ok(response)
    }

    @GetMapping("/{key}")
    @Operation(
        summary = "Get Tag By Key",
        description = """
            Get a tag by given `key`.
            
            You can find more information about tags [here](https://singularity.stereov.io/docs/guides/content/tags).
            
            ### Locale
            Tags can have multiple translations for `name` and `description`.
            You can specify a `locale` to generate the corresponding translation.
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/tags"),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The tag with `key`.",
            )
        ]
    )
    @ThrowsDomainError([
        FindDocumentByKeyException::class,
        TranslateException.NoTranslations::class
    ])
    suspend fun getTagByKey(
        @PathVariable key: String,
        @RequestParam locale: Locale?
    ): ResponseEntity<TagResponse> {
        val tag = service.findByKey(key)
            .getOrThrow { when (it) { is FindDocumentByKeyException -> it } }
        val response = tagMapper.createTagResponse(tag, locale)
            .getOrThrow { when (it) { is TranslateException.NoTranslations -> it } }

        return ResponseEntity.ok(response)
    }

    @GetMapping
    @Operation(
        summary = "Get Tags",
        description = """
            Get and filter existing tags.
            
            You can find more information about tags [here](https://singularity.stereov.io/docs/guides/content/tags).
            
            ### Locale
            Tags can have multiple translations for `name` and `description`.
            You can specify a `locale` to generate the corresponding translation.
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/tags"),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The tags.",
            ),
        ]
    )
    @ThrowsDomainError([
        FindAllDocumentsPaginatedException::class,
        TranslateException.NoTranslations::class
    ])
    suspend fun getTags(
        @RequestParam page: Int = 0,
        @RequestParam size: Int = 10,
        @RequestParam sort: List<String> = emptyList(),
        @RequestParam key: String?,
        @RequestParam name: String?,
        @RequestParam description: String?,
        @RequestParam locale: Locale?,
    ): ResponseEntity<PagedModel<TagResponse>> {
        val res = service.findAllPaginated(
            PageableRequest(page, size, sort).toPageable(),
            key,
            name,
            description,
            locale
        ).getOrThrow { when (it) { is FindAllDocumentsPaginatedException -> it } }
        val response = res.mapContent { tag ->
            tagMapper.createTagResponse(tag, locale)
                .getOrThrow { when (it) { is TranslateException.NoTranslations -> it } }
        }

        return ResponseEntity.ok(PagedModel(response))
    }

    @PatchMapping("/{key}")
    @Operation(
        summary = "Update Tag",
        description = """
            Update the tag with the given `key`.
            
            You can find more information about tags [here](https://singularity.stereov.io/docs/guides/content/tags).
            
            ### Locale
            The `locale` in the request body specifies which translation should be updated.
            The `locale` query parameter specifies which translation should be returned.
            
            The `locale` query parameter specifies the translation that will be returned.
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              from a member of the [`CONTRIBUTOR`](https://singularity.stereov.io/docs/guides/content/introduction#global-server-group-contributor) group.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/tags"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.CONTRIBUTOR_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.CONTRIBUTOR_SCOPE]),

        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The updated tag",
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        AuthenticationException.GroupMembershipRequired::class,
        UpdateTagException::class,
        TranslateException.NoTranslations::class
    ])
    suspend fun updateTag(
        @PathVariable key: String,
        @RequestBody req: UpdateTagRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<TagResponse> {
        authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            .requireGroupMembership(KnownGroups.CONTRIBUTOR)
            .getOrThrow { when (it) { is AuthenticationException.GroupMembershipRequired -> it } }

        val tag = service.updateTag(key, req)
            .getOrThrow { when (it) { is UpdateTagException -> it } }
        val response = tagMapper.createTagResponse(tag, locale)
            .getOrThrow { when (it) { is TranslateException.NoTranslations -> it } }

        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Delete Tag",
        description = """
            Delete the tag with the given `key`.
            
            You can find more information about tags [here](https://singularity.stereov.io/docs/guides/content/tags).
            
            ### Locale
            Tags can have multiple translations for `name` and `description`.
            You can specify a `locale` to generate the corresponding translation.
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              from a member of the [`CONTRIBUTOR`](https://singularity.stereov.io/docs/guides/content/introduction#global-server-group-contributor) group.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/tags"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.CONTRIBUTOR_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.CONTRIBUTOR_SCOPE]),

        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The updated tag",
            ),
        ]
    )
    @DeleteMapping("/{key}")
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        AuthenticationException.GroupMembershipRequired::class,
        DeleteDocumentByKeyException::class
    ])
    suspend fun deleteTag(@PathVariable key: String): ResponseEntity<SuccessResponse> {
        authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            .requireGroupMembership(KnownGroups.CONTRIBUTOR)
            .getOrThrow { when (it) { is AuthenticationException.GroupMembershipRequired -> it } }

        service.deleteByKey(key)
            .getOrThrow { when (it) { is DeleteDocumentByKeyException -> it } }
        return ResponseEntity.ok(SuccessResponse())
    }

}
