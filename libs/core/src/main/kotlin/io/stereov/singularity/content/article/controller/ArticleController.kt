package io.stereov.singularity.content.article.controller

import io.stereov.singularity.content.article.dto.response.ArticleOverviewResponse
import io.stereov.singularity.content.article.dto.response.ArticleResponse
import io.stereov.singularity.content.article.dto.response.FullArticleResponse
import io.stereov.singularity.content.article.service.ArticleService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
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
            service.getFullArticleResponseByKey(key, locale)
        )
    }


    @GetMapping("/scroll")
    suspend fun getLatestArticles(
        @RequestParam limit: Long = 10,
        @RequestParam after: String? = null,
        @RequestParam locale: Locale?
    ): ResponseEntity<ArticleResponse> {
        return ResponseEntity.ok(service.getAccessibleArticles(limit, after, locale))
    }

    @GetMapping
    suspend fun getArticles(
        @RequestParam page: Int = 0,
        @RequestParam size: Int = 10,
        @RequestParam tags: List<String> = emptyList(),
        @RequestParam locale: Locale?
    ): ResponseEntity<Page<ArticleOverviewResponse>> {
        val pageable = Pageable.ofSize(size).withPage(page)

        return ResponseEntity.ok(service.getArticles(pageable, tags, locale))
    }
}
