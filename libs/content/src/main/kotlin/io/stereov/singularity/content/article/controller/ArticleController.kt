package io.stereov.singularity.content.article.controller

import io.stereov.singularity.content.article.dto.ArticleResponse
import io.stereov.singularity.content.article.dto.ArticleTrustedResponse
import io.stereov.singularity.content.article.dto.FullArticleDto
import io.stereov.singularity.content.article.service.ArticleService
import io.stereov.singularity.content.article.service.UserArticleService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/api/content/articles")
class ArticleController(
    private val articleService: ArticleService,
    private val userArticleService: UserArticleService,
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

    @GetMapping
    suspend fun getLatestArticles(@RequestParam limit: Long = 10, @RequestParam after: String? = null): ResponseEntity<ArticleResponse> {
        return ResponseEntity.ok(userArticleService.getAccessibleArticles(limit, after))
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
