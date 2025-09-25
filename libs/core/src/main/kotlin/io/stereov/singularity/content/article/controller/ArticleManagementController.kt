package io.stereov.singularity.content.article.controller

import io.stereov.singularity.content.article.dto.request.*
import io.stereov.singularity.content.article.dto.response.FullArticleResponse
import io.stereov.singularity.content.article.service.ArticleManagementService
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/content/articles")
class ArticleManagementController(
    private val service: ArticleManagementService
) {

    @PostMapping
    suspend fun createArticle(
        @RequestBody req: CreateArticleRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(
            service.create(req, locale)
        )
    }

    @PutMapping("/{key}/header")
    suspend fun changeHeader(
        @PathVariable key: String,
        @RequestBody req: ChangeArticleHeaderRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(service.changeHeader(key, req, locale))
    }

    @PutMapping("/{key}/summary")
    suspend fun changeSummary(
        @PathVariable key: String,
        @RequestBody req: ChangeArticleSummaryRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(service.changeSummary(key, req, locale))
    }

    @PutMapping("/{key}/content")
    suspend fun changeContent(
        @PathVariable key: String,
        @RequestBody req: ChangeArticleContentRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(service.changeContent(key, req, locale))
    }

    @PutMapping("/{key}/image")
    suspend fun changeImage(
        @PathVariable key: String,
        @RequestPart file: FilePart,
        @RequestParam locale: Locale?
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok().body(
            service.changeImage(key, file, locale)
        )
    }

    @PutMapping("/{key}/state")
    suspend fun changeState(
        @PathVariable key: String,
        @RequestBody req: ChangeArticleStateRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(service.changeState(key, req, locale))
    }
}
