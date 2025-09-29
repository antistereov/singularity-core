package io.stereov.singularity.content.article.controller

import io.stereov.singularity.content.article.dto.response.ArticleOverviewResponse
import io.stereov.singularity.content.article.dto.response.FullArticleResponse
import io.stereov.singularity.content.article.service.ArticleService
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.PageableRequest
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.Page
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
            You can specify a `locale` to get the corresponding translation.
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/articles"),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The metadata of the file with `key`.",
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
                description = "No article with given key exists.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
        ]
    )
    suspend fun getArticleByKey(
        @PathVariable key: String,
        @RequestParam locale: Locale?
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(
            service.getResponseByKey(key, locale)
        )
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
            You can specify a `locale` to get the corresponding translation.
            
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
    suspend fun getArticles(
        @RequestParam page: Int = 0,
        @RequestParam size: Int = 10,
        @RequestParam sort: List<String> = emptyList(),
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
    ): ResponseEntity<Page<ArticleOverviewResponse>> {
        return ResponseEntity.ok(service.getArticles(
            PageableRequest(page, size, sort).toPageable(),
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
        ))
    }
}
