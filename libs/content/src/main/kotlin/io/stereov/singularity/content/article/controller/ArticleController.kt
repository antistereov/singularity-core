package io.stereov.singularity.content.article.controller

import io.stereov.singularity.content.article.dto.*
import io.stereov.singularity.content.article.service.ArticleManagementService
import io.stereov.singularity.content.article.service.ArticleService
import io.stereov.singularity.core.auth.service.AuthenticationService
import io.stereov.singularity.core.global.language.model.Language
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
    private val authenticationService: AuthenticationService,
) {

    @GetMapping("/{key}")
    suspend fun getArticleByKey(
        @PathVariable key: String,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(
            articleService.getFullArticleResponseByKey(key, lang)
        )
    }

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
    suspend fun getLatestArticles(
        @RequestParam limit: Long = 10,
        @RequestParam after: String? = null,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<ArticleResponse> {
        return ResponseEntity.ok(articleManagementService.getAccessibleArticles(limit, after, lang))
    }

    @GetMapping
    suspend fun getArticles(
        @RequestParam page: Int = 0,
        @RequestParam size: Int = 10,
        @RequestParam tags: List<String> = emptyList(),
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<Page<ArticleOverviewResponse>> {
        val pageable = Pageable.ofSize(size).withPage(page)
        val currentUser = authenticationService.getCurrentUserOrNull()

        return ResponseEntity.ok(
            articleService.getArticles(pageable, tags).map { it.toOverviewResponse(lang, currentUser) }
        )
    }

    @PostMapping("/create")
    suspend fun createArticle(
        @RequestBody req: CreateArticleRequest,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(
            articleService.create(req, lang)
        )
    }
}
