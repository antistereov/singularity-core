package io.stereov.singularity.content.article.controller

import io.stereov.singularity.content.article.dto.*
import io.stereov.singularity.content.article.service.ArticleManagementService
import io.stereov.singularity.content.core.dto.*
import io.stereov.singularity.content.core.dto.AcceptInvitationToContentRequest
import io.stereov.singularity.content.translate.model.Language
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*

@RestController
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

    @PutMapping("/{key}/header")
    suspend fun changeHeader(
        @PathVariable key: String,
        @RequestBody req: ChangeArticleHeaderRequest,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(service.changeHeader(key, req, lang))
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

    @PutMapping("/{key}/image")
    suspend fun changeImage(
        @PathVariable key: String,
        @RequestPart file: FilePart,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok().body(
            service.changeImage(key, file, lang)
        )
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

    @PostMapping("/{key}/invite")
    suspend fun inviteUser(
        @PathVariable key: String,
        @RequestBody req: InviteUserToContentRequest,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<ExtendedContentAccessDetailsResponse> {
        return ResponseEntity.ok(service.inviteUser(key, req, lang))
    }

    @PostMapping("/invite/accept")
    suspend fun acceptInvitation(
        @RequestBody req: AcceptInvitationToContentRequest,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<FullArticleResponse> {
        return ResponseEntity.ok(service.acceptInvitationAndGetFullArticle(req, lang))
    }

    @GetMapping("/{key}/access")
    suspend fun getExtendedAccessDetails(
        @PathVariable key: String
    ): ResponseEntity<ExtendedContentAccessDetailsResponse> {
        return ResponseEntity.ok(service.extendedContentAccessDetails(key))
    }
}
