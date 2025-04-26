package io.stereov.singularity.stereovio.content.article.controller

import io.stereov.singularity.stereovio.content.article.dto.FullArticleDto
import io.stereov.singularity.stereovio.content.article.dto.ArticleOverviewDto
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

    @GetMapping("{key}")
    suspend fun findByKey(@PathVariable key: String): ResponseEntity<FullArticleDto> {
        return ResponseEntity.ok().body(
            articleService.findByKey(key).toContentDto()
        )
    }

    @GetMapping("/latest")
    suspend fun getLatestArticles(@RequestParam limit: Long = 10): ResponseEntity<List<ArticleOverviewDto>> {
        return ResponseEntity.ok(articleService.getLatestArticles(limit).map { it.toSlideDto() })
    }

    @GetMapping("/next")
    suspend fun getArticles(
        @RequestParam id: String,
        @RequestParam limit: Long = 10
    ): ResponseEntity<List<ArticleOverviewDto>> {
        return ResponseEntity.ok(articleService.getNextArticles(id, limit).map { it.toSlideDto() })
    }
}
