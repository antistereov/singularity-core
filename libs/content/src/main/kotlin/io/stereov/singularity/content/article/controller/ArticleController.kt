package io.stereov.singularity.content.article.controller

import io.stereov.singularity.content.article.dto.ArticleOverviewResponse
import io.stereov.singularity.content.article.dto.ArticleResponse
import io.stereov.singularity.content.article.dto.FullArticleResponse
import io.stereov.singularity.content.article.service.ArticleService
import io.stereov.singularity.global.language.model.Language
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/api/content/articles")
class ArticleController(
    private val service: ArticleService,
) {

    @GetMapping("/{key}")
    suspend fun getArticleByKey(
        @PathVariable key: String,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(
            service.getFullArticleResponseByKey(key, lang)
        )
    }


    @GetMapping("/scroll")
    suspend fun getLatestArticles(
        @RequestParam limit: Long = 10,
        @RequestParam after: String? = null,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<ArticleResponse> {
        return ResponseEntity.ok(service.getAccessibleArticles(limit, after, lang))
    }

    @GetMapping
    suspend fun getArticles(
        @RequestParam page: Int = 0,
        @RequestParam size: Int = 10,
        @RequestParam tags: List<String> = emptyList(),
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<Page<ArticleOverviewResponse>> {
        val pageable = Pageable.ofSize(size).withPage(page)

        return ResponseEntity.ok(service.getArticles(pageable, tags, lang))
    }
}
