package io.stereov.singularity.content.core.controller

import io.stereov.singularity.content.core.dto.request.ChangeContentTagsRequest
import io.stereov.singularity.content.core.dto.request.ChangeContentVisibilityRequest
import io.stereov.singularity.content.core.dto.response.ContentResponse
import io.stereov.singularity.content.core.dto.response.ExtendedContentAccessDetailsResponse
import io.stereov.singularity.content.core.util.findContentManagementService
import io.stereov.singularity.global.model.SuccessResponse
import org.springframework.context.ApplicationContext
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/content")
class ContentManagementController(
    private val context: ApplicationContext
) {

    @PutMapping("/{contentType}/{key}/tags")
    suspend fun changeTags(
        @PathVariable key: String,
        @PathVariable contentType: String,
        @RequestBody req: ChangeContentTagsRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<out ContentResponse<*>> {
        return ResponseEntity.ok(
            context.findContentManagementService(contentType).changeTags(key, req, locale)
        )
    }

    @PutMapping("/{contentType}/{key}/visibility")
    suspend fun changeVisibility(
        @PathVariable key: String,
        @PathVariable contentType: String,
        @RequestBody req: ChangeContentVisibilityRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<out ContentResponse<*>> {
        return ResponseEntity.ok(
            context.findContentManagementService(contentType).changeVisibility(key, req, locale)
        )
    }

    @GetMapping("/{contentType}/{key}/access")
    suspend fun getExtendedAccessDetails(
        @PathVariable contentType: String,
        @PathVariable key: String
    ): ResponseEntity<ExtendedContentAccessDetailsResponse> {
        return ResponseEntity.ok(
            context.findContentManagementService(contentType).extendedContentAccessDetails(key)
        )
    }

    @DeleteMapping("/{contentType}/{key}")
    suspend fun deleteContentByKey(
        @PathVariable contentType: String,
        @PathVariable key: String
    ): ResponseEntity<SuccessResponse> {
        context.findContentManagementService(contentType).deleteByKey(key)

        return ResponseEntity.ok(SuccessResponse())
    }

    @PutMapping("/{contentType}/{key}/trusted")
    suspend fun setTrustedState(
        @PathVariable contentType: String,
        @PathVariable key: String,
        @RequestParam trusted: Boolean,
        @RequestParam locale: Locale?
    ): ResponseEntity<ContentResponse<*>> {
        val res = context.findContentManagementService(contentType).setTrustedState(key, trusted, locale)
        return ResponseEntity.ok(res)
    }
}