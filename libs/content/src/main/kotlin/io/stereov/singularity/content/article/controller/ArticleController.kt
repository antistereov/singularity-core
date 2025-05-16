package io.stereov.singularity.content.article.controller

import io.stereov.singularity.content.article.dto.*
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.service.ArticleManagementService
import io.stereov.singularity.content.article.service.ArticleService
import io.stereov.singularity.content.common.controller.ContentController
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/api/content/articles")
class ArticleController(
    private val articleService: ArticleService,
    private val articleManagementService: ArticleManagementService,
) : ContentController<Article>(articleService, articleManagementService) {

    @GetMapping("/{key}/trusted")
    suspend fun isArticleTrusted(@PathVariable key: String): ResponseEntity<ArticleTrustedResponse> {
        return ResponseEntity.ok(
            ArticleTrustedResponse(articleService.findByKey(key).trusted)
        )
    }

    @PutMapping("/{key}/trusted")
    suspend fun setTrustedState(@PathVariable key: String, @RequestParam trusted: Boolean): ResponseEntity<ArticleTrustedResponse> {
        articleService.setTrustedState(key, trusted)
        return ResponseEntity.ok(
            ArticleTrustedResponse(trusted)
        )
    }

    @GetMapping("/scroll")
    suspend fun getLatestArticles(@RequestParam limit: Long = 10, @RequestParam after: String? = null): ResponseEntity<ArticleResponse> {
        return ResponseEntity.ok(articleManagementService.getAccessibleArticles(limit, after))
    }

    @GetMapping
    suspend fun getArticles(@RequestParam page: Int = 0, @RequestParam size: Int = 10): ResponseEntity<Page<ArticleOverviewResponse>> {
        val pageable = Pageable.ofSize(size).withPage(page)
        return ResponseEntity.ok(
            articleManagementService.findAccessible(pageable).map { it.toOverviewResponse() }
        )
    }

    @PostMapping("/create")
    suspend fun createArticle(@RequestBody req: CreateArticleRequest): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(
            articleService.create(req)
        )
    }
}
