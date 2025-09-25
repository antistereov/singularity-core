package io.stereov.singularity.content.article.controller

import io.stereov.singularity.content.article.dto.response.ArticleOverviewResponse
import io.stereov.singularity.content.article.dto.response.FullArticleResponse
import io.stereov.singularity.content.article.service.ArticleService
import io.stereov.singularity.global.model.PageableRequest
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/content/articles")
class ArticleController(
    private val service: ArticleService,
) {

    @GetMapping("/{key}")
    suspend fun getArticleByKey(
        @PathVariable key: String,
        @RequestParam locale: Locale?
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(
            service.getResponseByKey(key, locale)
        )
    }

    @GetMapping
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
