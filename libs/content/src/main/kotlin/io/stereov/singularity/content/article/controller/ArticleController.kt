package io.stereov.singularity.content.article.controller

import io.stereov.singularity.content.article.dto.ArticleOverviewDto
import io.stereov.singularity.content.article.dto.ArticleTrustedResponse
import io.stereov.singularity.content.article.dto.FullArticleDto
import io.stereov.singularity.content.article.service.ArticleService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/api/content/articles")
class ArticleController(
    private val articleService: ArticleService,
) {

    @GetMapping("{key}")
    suspend fun findByKey(@PathVariable key: String): ResponseEntity<FullArticleDto> {
        return ResponseEntity.ok().body(
            articleService.findFullArticleDtoByKey(key)
        )
    }

    @GetMapping("/trusted/{key}")
    suspend fun isArticleTrusted(@PathVariable key: String): ResponseEntity<ArticleTrustedResponse> {
        return ResponseEntity.ok(
            ArticleTrustedResponse(articleService.findByKey(key).trusted)
        )
    }

    @PutMapping("/trusted/{key}")
    suspend fun setTrustedState(@PathVariable key: String, @RequestParam trusted: Boolean): ResponseEntity<ArticleTrustedResponse> {
        articleService.setTrustedState(key, trusted)
        return ResponseEntity.ok(
            ArticleTrustedResponse(trusted)
        )
    }

    @GetMapping("/latest")
    suspend fun getLatestArticles(@RequestParam limit: Long = 10): ResponseEntity<List<ArticleOverviewDto>> {
        return ResponseEntity.ok(articleService.getLatestArticles(limit).map { it.toOverviewDto() })
    }

    @GetMapping("/next")
    suspend fun getArticles(
        @RequestParam id: String,
        @RequestParam limit: Long = 10
    ): ResponseEntity<List<ArticleOverviewDto>> {
        return ResponseEntity.ok(articleService.getNextArticles(id, limit).map { it.toOverviewDto() })
    }

    @PutMapping
    suspend fun saveArticle(
        @RequestBody article: FullArticleDto
    ): ResponseEntity<FullArticleDto> {
        return ResponseEntity.ok(
            articleService.save(article)
        )
    }
}
