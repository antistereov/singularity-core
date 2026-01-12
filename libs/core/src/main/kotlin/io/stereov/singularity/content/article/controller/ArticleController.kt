package io.stereov.singularity.content.article.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.token.exception.AccessTokenExtractionException
import io.stereov.singularity.content.article.dto.response.ArticleOverviewResponse
import io.stereov.singularity.content.article.dto.response.FullArticleResponse
import io.stereov.singularity.content.article.exception.GetArticleResponseByKeyException
import io.stereov.singularity.content.article.exception.GetArticleResponsesException
import io.stereov.singularity.content.article.service.ArticleService
import io.stereov.singularity.global.annotation.ThrowsDomainError
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/content/articles")
@Tag(name = "Articles", description = "Operations related to articles")
@ConditionalOnProperty(prefix = "singularity.content.articles", value = ["enable"], havingValue = "true", matchIfMissing = true)
class ArticleController(
    private val service: ArticleService,
    private val authorizationService: AuthorizationService,
) {

    @GetMapping("/{key}")
    @Operation(
        summary = "Get Article By Key",
        description = """
            Get an article by given `key`.
            
            You can find more information about articles [here](https://singularity.stereov.io/docs/guides/content/articles).
            
            >**Note:** It will only return articles that are accessible by the requester. 
            >You can learn more about access [here](https://singularity.stereov.io/docs/guides/content/introduction#authorization-logic).
            
            ### Locale
            Tags can have multiple translations for `name` and `description`.
            You can specify a `locale` to generate the corresponding translation.
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/articles"),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The metadata of the file with `key`.",
            ),
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        GetArticleResponseByKeyException::class
    ])
    suspend fun getArticleByKey(
        @PathVariable key: String,
        @RequestParam locale: Locale?
    ): ResponseEntity<FullArticleResponse> {
        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }

        val response = service.getResponseByKey(key, authenticationOutcome, locale)
            .getOrThrow { when (it) { is GetArticleResponseByKeyException -> it } }

        return ResponseEntity.ok(response)
    }

    @GetMapping
    @Operation(
        summary = "Get Articles",
        description = """
            Get and filter articles.
            
            You can find more information about articles [here](https://singularity.stereov.io/docs/guides/content/articles).
            
            >**Note:** It will only return articles that are accessible by the requester. 
            >You can learn more about access [here](https://singularity.stereov.io/docs/guides/content/introduction#authorization-logic).
            
            ### Locale
            Tags can have multiple translations for `name` and `description`.
            You can specify a `locale` to generate the corresponding translation.
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/articles"),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The metadata of the file with `key`.",
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        GetArticleResponsesException::class
    ])
    suspend fun getArticles(
        pageable: Pageable,
        @RequestParam tags: List<String> = emptyList(),
        @RequestParam title: String? = null,
        @RequestParam content: String? = null,
        @RequestParam state: String? = null,
        @RequestParam roles: Set<String> = emptySet(),
        @RequestParam createdAtBefore: Instant?,
        @RequestParam createdAtAfter: Instant?,
        @RequestParam updatedAtBefore: Instant?,
        @RequestParam updatedAtAfter: Instant?,
        @RequestParam publishedAtBefore: Instant?,
        @RequestParam publishedAtAfter: Instant?,
        @RequestParam locale: Locale?,
    ): ResponseEntity<PagedModel<ArticleOverviewResponse>> {
        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }

        val response = service.getArticles(
            pageable,
            authenticationOutcome,
            title,
            content,
            state,
            tags,
            roles,
            createdAtBefore,
            createdAtAfter,
            updatedAtBefore,
            updatedAtAfter,
            publishedAtBefore,
            publishedAtAfter,
            locale
        ).getOrThrow { when (it) { is GetArticleResponsesException -> it } }

        return ResponseEntity.ok(PagedModel(response))
    }
}
