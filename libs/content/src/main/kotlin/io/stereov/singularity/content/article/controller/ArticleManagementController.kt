package io.stereov.singularity.content.article.controller

import io.stereov.singularity.content.article.dto.*
import io.stereov.singularity.content.article.service.ArticleManagementService
import io.stereov.singularity.content.common.content.dto.ChangeContentTagsRequest
import io.stereov.singularity.content.common.content.dto.ChangeContentVisibilityRequest
import io.stereov.singularity.core.global.language.model.Language
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/api/content/articles")
class ArticleManagementController(
    private val service: ArticleManagementService
) {

    @PostMapping("/create")
    suspend fun createArticle(
        @RequestBody req: CreateArticleRequest,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(
            service.create(req, lang)
        )
    }

    @PutMapping("/{key}/trusted")
    suspend fun setTrustedState(@PathVariable key: String, @RequestParam trusted: Boolean): ResponseEntity<ArticleTrustedResponse> {
        service.setTrustedState(key, trusted)
        return ResponseEntity.ok(
            ArticleTrustedResponse(trusted)
        )
    }

    @PutMapping("/{key}/title")
    suspend fun changeTitle(
        @PathVariable key: String,
        @RequestBody req: ChangeArticleTitleRequest,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(service.changeTitle(key, req, lang))
    }

    @PutMapping("/{key}/summary")
    suspend fun changeSummary(
        @PathVariable key: String,
        @RequestBody req: ChangeArticleSummaryRequest,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(service.changeSummary(key, req, lang))
    }

    @PutMapping("/{key}/content")
    suspend fun changeContent(
        @PathVariable key: String,
        @RequestBody req: ChangeArticleContentRequest,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(service.changeContent(key, req, lang))
    }

    @PutMapping("/{key}/colors")
    suspend fun changeColors(
        @PathVariable key: String,
        @RequestBody req: ChangeArticleColorsRequest,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(service.changeColors(key, req, lang))
    }

    @PutMapping("/{key}/image")
    suspend fun changeImage(
        @PathVariable key: String,
        @RequestBody req: ChangeArticleImageRequest,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(service.changeImage(key, req, lang))
    }

    @PutMapping("/{key}/state")
    suspend fun changeState(
        @PathVariable key: String,
        @RequestBody req: ChangeArticleStateRequest,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(service.changeState(key, req, lang))
    }

    @PutMapping("/{key}/tags")
    suspend fun changeTags(
        @PathVariable key: String,
        @RequestBody req: ChangeContentTagsRequest,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(service.changeTags(key, req, lang))
    }

    @PutMapping("/{key}/visibility")
    suspend fun changeVisibility(
        @PathVariable key: String,
        @RequestBody req: ChangeContentVisibilityRequest,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(service.changeVisibility(key, req, lang))
    }
}
