package io.stereov.singularity.content.article.controller

import io.stereov.singularity.content.article.dto.*
import io.stereov.singularity.content.article.dto.request.ChangeArticleContentRequest
import io.stereov.singularity.content.article.dto.request.ChangeArticleHeaderRequest
import io.stereov.singularity.content.article.dto.request.ChangeArticleStateRequest
import io.stereov.singularity.content.article.dto.request.ChangeArticleSummaryRequest
import io.stereov.singularity.content.article.dto.request.CreateArticleRequest
import io.stereov.singularity.content.article.dto.response.ArticleTrustedResponse
import io.stereov.singularity.content.article.dto.response.FullArticleResponse
import io.stereov.singularity.content.article.service.ArticleManagementService
import io.stereov.singularity.content.core.dto.*
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/content/articles")
class ArticleManagementController(
    private val service: ArticleManagementService
) {

    @PostMapping("/create")
    suspend fun createArticle(
        @RequestBody req: CreateArticleRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(
            service.create(req, locale)
        )
    }

    @PutMapping("/{key}/trusted")
    suspend fun setTrustedState(@PathVariable key: String, @RequestParam trusted: Boolean): ResponseEntity<ArticleTrustedResponse> {
        service.setTrustedState(key, trusted)
        return ResponseEntity.ok(
            ArticleTrustedResponse(trusted)
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

    @PutMapping("/{key}/tags")
    suspend fun changeTags(
        @PathVariable key: String,
        @RequestBody req: ChangeContentTagsRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(service.changeTags(key, req, locale))
    }

    @PutMapping("/{key}/visibility")
    suspend fun changeVisibility(
        @PathVariable key: String,
        @RequestBody req: ChangeContentVisibilityRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(service.changeVisibility(key, req, locale))
    }

    @PostMapping("/{key}/invite")
    suspend fun inviteUser(
        @PathVariable key: String,
        @RequestBody req: InviteUserToContentRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<ExtendedContentAccessDetailsResponse> {
        return ResponseEntity.ok(service.inviteUser(key, req, locale))
    }

    @PostMapping("/invite/accept")
    suspend fun acceptInvitation(
        @RequestBody req: AcceptInvitationToContentRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(service.acceptInvitationAndGetFullArticle(req, locale))
    }

    @GetMapping("/{key}/access")
    suspend fun getExtendedAccessDetails(
        @PathVariable key: String
    ): ResponseEntity<ExtendedContentAccessDetailsResponse> {
        return ResponseEntity.ok(service.extendedContentAccessDetails(key))
    }
}
