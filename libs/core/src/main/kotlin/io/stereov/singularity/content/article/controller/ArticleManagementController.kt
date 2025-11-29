package io.stereov.singularity.content.article.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.token.exception.AccessTokenExtractionException
import io.stereov.singularity.content.article.dto.request.ChangeArticleStateRequest
import io.stereov.singularity.content.article.dto.request.CreateArticleRequest
import io.stereov.singularity.content.article.dto.request.UpdateArticleRequest
import io.stereov.singularity.content.article.dto.response.FullArticleResponse
import io.stereov.singularity.content.article.exception.ChangeArticleImageException
import io.stereov.singularity.content.article.exception.ChangeArticleStateException
import io.stereov.singularity.content.article.exception.CreateArticleException
import io.stereov.singularity.content.article.exception.UpdateArticleException
import io.stereov.singularity.content.article.service.ArticleManagementService
import io.stereov.singularity.global.annotation.ThrowsDomainError
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.principal.group.model.KnownGroups
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
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
    private val authorizationService: AuthorizationService,
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
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        AuthenticationException.GroupMembershipRequired::class,
        CreateArticleException::class,
    ])
    suspend fun createArticle(
        @RequestBody req: CreateArticleRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<FullArticleResponse> {
        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            .requireGroupMembership(KnownGroups.CONTRIBUTOR)
            .getOrThrow { when (it) { is AuthenticationException.GroupMembershipRequired -> it } }

        val response = service.create(req, authenticationOutcome, locale)
            .getOrThrow { when (it) { is CreateArticleException -> it } }

        return ResponseEntity.ok(response)
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
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        UpdateArticleException::class,
    ])
    suspend fun updateArticle(
        @PathVariable key: String,
        @RequestBody req: UpdateArticleRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<FullArticleResponse> {
        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }

        val response = service.updateArticle(key, req, authenticationOutcome, locale)
            .getOrThrow { when (it) { is UpdateArticleException -> it } }

        return ResponseEntity.ok(response)
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
        ]
    )
    suspend fun updateArticleImage(
        @PathVariable key: String,
        @RequestPart file: FilePart,
        @RequestParam locale: Locale?
    ): ResponseEntity<FullArticleResponse> {
        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }

        val response = service.changeImage(key, file, authenticationOutcome, locale)
            .getOrThrow { when (it) { is ChangeArticleImageException -> it } }

        return ResponseEntity.ok().body(response)
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
            )
        ]
    )
    suspend fun updateArticleState(
        @PathVariable key: String,
        @RequestBody req: ChangeArticleStateRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<FullArticleResponse> {
        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }

        val response = service.changeState(key, req, authenticationOutcome, locale)
            .getOrThrow { when (it) { is ChangeArticleStateException -> it } }

        return ResponseEntity.ok(response)
    }
}
