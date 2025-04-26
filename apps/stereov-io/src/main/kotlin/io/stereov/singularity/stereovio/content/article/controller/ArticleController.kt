package io.stereov.singularity.stereovio.content.article.controller

import io.stereov.singularity.stereovio.content.article.dto.ArticleDto
import io.stereov.singularity.stereovio.content.article.model.Article
import io.stereov.singularity.stereovio.content.article.service.ArticleService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("content/articles")
class ArticleController(
    private val articleService: ArticleService,
) {

    @GetMapping("{id}")
    suspend fun findById(@PathVariable id: String): ResponseEntity<ArticleDto> {
        return ResponseEntity.ok().body(
            articleService.findById(id).toDto()
        )
    }

    @GetMapping("/latest")
    suspend fun getLatestArticles(@RequestParam limit: Long = 10): ResponseEntity<List<Article>> {
        return ResponseEntity.ok(articleService.getLatestArticles(limit))
    }

    @GetMapping("/next")
    suspend fun getArticles(
        @RequestParam id: String,
        @RequestParam limit: Long = 10
    ): ResponseEntity<List<Article>> {
        return ResponseEntity.ok(articleService.getNextArticles(id, limit))
    }
}
