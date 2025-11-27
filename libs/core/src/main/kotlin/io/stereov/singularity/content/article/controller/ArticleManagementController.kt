package io.stereov.singularity.content.article.controller

import io.stereov.singularity.content.article.dto.request.ChangeArticleStateRequest
import io.stereov.singularity.content.article.dto.request.CreateArticleRequest
import io.stereov.singularity.content.article.dto.request.UpdateArticleRequest
import io.stereov.singularity.content.article.dto.response.FullArticleResponse
import io.stereov.singularity.content.article.service.ArticleManagementService
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/content/articles")
@Tag(name = "Articles")
@ConditionalOnProperty(prefix = "singularity.content.articles", value = ["enable"], havingValue = "true", matchIfMissing = true)
class ArticleManagementController(
    private val service: ArticleManagementService,
) {

    @PostMapping
    @Operation(
        summary = "Create Article",
        description = """
            Create a new article.
            
            You can find more information about articles [here](https://singularity.stereov.io/docs/guides/content/articles).
            
            ### Locale
            The `locale` in the request body specifies which translation should be created.
            The `locale` query parameter specifies which translation should be returned.
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              from a member of the [`CONTRIBUTOR`](https://singularity.stereov.io/docs/guides/content/introduction#global-server-group-contributor) group.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/articles"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.CONTRIBUTOR_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.CONTRIBUTOR_SCOPE]),

        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The newly created article.",
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired `AccessToken`.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "`AccessToken` does not contain group membership " +
                        "[`CONTRIBUTOR`](https://singularity.stereov.io/docs/guides/content/introduction#global-server-group-contributor) access.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "An article with the given `key` already exists.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun createArticle(
        @RequestBody req: CreateArticleRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(
            service.create(req, locale)
        )
    }

    @PatchMapping ("/{key}")
    @Operation(
        summary = "Update Article",
        description = """
            Update an existing article with given `key`.
            
            You can find more information about articles [here](https://singularity.stereov.io/docs/guides/content/articles).
            
            ### Locale
            The `locale` in the request body specifies which translation should be updated.
            The `locale` query parameter specifies which translation should be returned.

            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              of a user with [`EDITOR`](https://singularity.stereov.io/docs/guides/content/introduction#object-specific-roles-shared-state) access on this article is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/articles"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.EDITOR_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.EDITOR_SCOPE]),

        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The updated article.",
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired `AccessToken`.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "`AccessToken` does not belong to a user with " +
                        "[`EDITOR`](https://singularity.stereov.io/docs/guides/content/introduction#object-specific-roles-shared-state) " +
                        "access on this article.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "No article with the given `key` exists.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun updateArticle(
        @PathVariable key: String,
        @RequestBody req: UpdateArticleRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(service.updateArticle(key, req, locale))
    }

    @PutMapping("/{key}/image")
    @Operation(
        summary = "Update Article Image",
        description = """
            Update the image of an existing article with given `key`.
            
            You can find more information about articles [here](https://singularity.stereov.io/docs/guides/content/articles).
            
            ### Locale
            The `locale` in the request body specifies which translation should be created.
            The `locale` query parameter specifies which translation should be returned.
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              of a user with [`EDITOR`](https://singularity.stereov.io/docs/guides/content/introduction#object-specific-roles-shared-state) access on this article is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/articles"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.EDITOR_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.EDITOR_SCOPE]),

        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The updated article.",
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired `AccessToken`.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "`AccessToken` does not belong to a user with " +
                        "[`EDITOR`](https://singularity.stereov.io/docs/guides/content/introduction#object-specific-roles-shared-state) " +
                        "access on this article.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "No article with the given `key` exists.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "413",
                description = "File is too large.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
        ]
    )
    suspend fun updateArticleImage(
        @PathVariable key: String,
        @RequestPart file: FilePart,
        @RequestParam locale: Locale?
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok().body(
            service.changeImage(key, file, locale)
        )
    }

    @PutMapping("/{key}/state")
    @Operation(
        summary = "Update Article State",
        description = """
            Update the state of an existing article with given `key`.
            
            You can find more information about articles [here](https://singularity.stereov.io/docs/guides/content/articles).
            
            ### Locale
            Tags can have multiple translations for `name` and `description`.
            You can specify a `locale` to generate the corresponding translation.
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              of a user with [`EDITOR`](https://singularity.stereov.io/docs/guides/content/introduction#object-specific-roles-shared-state) access on this article is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/articles"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.EDITOR_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.EDITOR_SCOPE]),

        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The updated article.",
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired `AccessToken`.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "`AccessToken` does not belong to a user with " +
                        "[`EDITOR`](https://singularity.stereov.io/docs/guides/content/introduction#object-specific-roles-shared-state) " +
                        "access on this article.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "No article with the given `key` exists.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun updateArticleState(
        @PathVariable key: String,
        @RequestBody req: ChangeArticleStateRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(service.changeState(key, req, locale))
    }
}
