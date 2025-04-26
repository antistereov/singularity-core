package io.stereov.singularity.stereovio.content.article.controller

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
    suspend fun findById(@PathVariable id: String): ResponseEntity<Article> {
        return ResponseEntity.ok().body(
            articleService.findById(id)
        )
    }

    @GetMapping
    suspend fun getArticles(
        @RequestParam page: Int = 0,
        @RequestParam size: Int = 10
    ): ResponseEntity<List<Article>> {
        return ResponseEntity.ok(articleService.getArticlesPaged(page, size))
    }
}
