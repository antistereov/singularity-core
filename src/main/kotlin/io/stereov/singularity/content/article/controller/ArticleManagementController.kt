package io.stereov.singularity.content.article.controller

import io.stereov.singularity.content.article.dto.request.ChangeArticleStateRequest
import io.stereov.singularity.content.article.dto.request.CreateArticleRequest
import io.stereov.singularity.content.article.dto.request.UpdateArticleRequest
import io.stereov.singularity.content.article.dto.response.FullArticleResponse
import io.stereov.singularity.content.article.service.ArticleManagementService
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/content/articles")
class ArticleManagementController(
    private val service: ArticleManagementService,
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

    @PatchMapping ("/{key}")
    suspend fun updateArticle(
        @PathVariable key: String,
        @RequestBody req: UpdateArticleRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(service.updateArticle(key, req, locale))
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
