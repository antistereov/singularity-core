package io.stereov.singularity.content.tag.controller

import io.stereov.singularity.content.tag.dto.CreateTagRequest
import io.stereov.singularity.content.tag.dto.TagResponse
import io.stereov.singularity.content.tag.dto.UpdateTagRequest
import io.stereov.singularity.content.tag.mapper.TagMapper
import io.stereov.singularity.content.tag.service.TagService
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.global.model.PageableRequest
import io.stereov.singularity.global.model.SuccessResponse
import io.stereov.singularity.global.util.mapContent
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/content/tags")
@Tag(name = "Tags", description = "Operations related to tags.")
class TagController(
    private val service: TagService,
    private val tagMapper: TagMapper
) {

    @PostMapping
    @Operation(
        summary = "Create Tag",
        description = """
            Create a new tag.
            
            You can find more information about tags [here](https://singularity.stereov.io/docs/guides/content/tags).
            
            **Locale:**
            Tags can have multiple translations for `name` and `description`.
            You can specify a `locale` to get the corresponding translation.
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            **Tokens:**
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) permissions is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/tags"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.ADMIN_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.ADMIN_SCOPE]),

        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The newly created tag.",
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired `AccessToken`.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "`AccessToken` does permit [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) access.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "A tag with the given `key` already exists.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun createTag(@RequestBody req: CreateTagRequest): ResponseEntity<TagResponse> {
        return ResponseEntity.ok(
            tagMapper.createTagResponse(service.create(req), req.locale)
        )
    }

    @GetMapping("/{key}")
    @Operation(
        summary = "Find Tag By Key",
        description = """
            Find a tag by given `key`.
            
            You can find more information about tags [here](https://singularity.stereov.io/docs/guides/content/tags).
            
            **Locale:**
            Tags can have multiple translations for `name` and `description`.
            You can specify a `locale` to get the corresponding translation.
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/tags"),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The tag with `key`.",
            ),
            ApiResponse(
                responseCode = "404",
                description = "No tag with given key exists.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
        ]
    )
    suspend fun findTagByKey(
        @PathVariable key: String,
        @RequestParam locale: Locale?
    ): ResponseEntity<TagResponse> {
        return ResponseEntity.ok(
            tagMapper.createTagResponse(service.findByKey(key), locale)
        )
    }

    @GetMapping
    @Operation(
        summary = "Find Tags",
        description = """
            Find and filter existing tags.
            
            You can find more information about tags [here](https://singularity.stereov.io/docs/guides/content/tags).
            
            **Locale:**
            Tags can have multiple translations for `name` and `description`.
            You can specify a `locale` to get the corresponding translation.
            
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
    suspend fun findTags(
        @RequestParam page: Int = 0,
        @RequestParam size: Int = 10,
        @RequestParam sort: List<String> = emptyList(),
        @RequestParam key: String?,
        @RequestParam name: String?,
        @RequestParam description: String?,
        @RequestParam locale: Locale?,
    ): ResponseEntity<Page<TagResponse>> {
        val res = service.findAllPaginated(
            PageableRequest(page, size, sort).toPageable(),
            key,
            name,
            description,
            locale
        )

        return ResponseEntity.ok(res.mapContent { tagMapper.createTagResponse(it, locale) })
    }

    @PutMapping("/{key}")
    @Operation(
        summary = "Update Tag",
        description = """
            Update the tag with the given `key`.
            
            You can find more information about tags [here](https://singularity.stereov.io/docs/guides/content/tags).
            
            **Locale:**
            Tags can have multiple translations for `name` and `description`.
            The request body can take multiple translations.
            
            The `locale` query parameter specifies the translation that will be returned.
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            **Tokens:**
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) permissions is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/tags"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.ADMIN_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.ADMIN_SCOPE]),

        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The updated tag",
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired `AccessToken`.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "`AccessToken` does permit [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) access.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "No tag with `key` found.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun updateTag(
        @PathVariable key: String,
        @RequestBody req: UpdateTagRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<TagResponse> {
        return ResponseEntity.ok(tagMapper.createTagResponse(service.updateTag(key, req), locale))
    }

    @Operation(
        summary = "Delete Tag",
        description = """
            Delete the tag with the given `key`.
            
            You can find more information about tags [here](https://singularity.stereov.io/docs/guides/content/tags).
            
            **Locale:**
            Tags can have multiple translations for `name` and `description`.
            You can specify a `locale` to get the corresponding translation.
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            **Tokens:**
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) permissions is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/tags"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.ADMIN_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.ADMIN_SCOPE]),

        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The updated tag",
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired `AccessToken`.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "`AccessToken` does permit [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) access.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "No tag with `key` found.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @DeleteMapping("/{key}")
    suspend fun deleteTag(@PathVariable key: String): ResponseEntity<SuccessResponse> {
        return ResponseEntity.ok(SuccessResponse(service.deleteByKey(key)))
    }

}
