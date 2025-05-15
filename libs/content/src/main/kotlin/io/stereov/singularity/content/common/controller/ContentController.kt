package io.stereov.singularity.content.common.controller

import io.stereov.singularity.content.common.model.ContentDocument
import io.stereov.singularity.content.common.service.ContentManagementService
import io.stereov.singularity.content.common.service.ContentService
import io.stereov.singularity.core.global.model.ExistsResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam

abstract class ContentController<T: ContentDocument<T>>(
    private val contentService: ContentService<T>,
    private val contentManagementService: ContentManagementService<T>
) {

    suspend fun findByKey(@PathVariable key: String): T {
        return contentService.findByKey(key)
    }

    suspend fun existsByKey(@PathVariable key: String): ResponseEntity<ExistsResponse> {
        return ResponseEntity.ok(ExistsResponse(contentService.existsByKey(key)))
    }

    open suspend fun findAccessible(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Page<T>> {
        val pageable = PageRequest.of(page, size)
        return ResponseEntity.ok(contentManagementService.findAccessible(pageable))
    }

    suspend fun findEditable(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Page<T>> {
        val pageable = PageRequest.of(page, size)
        return ResponseEntity.ok(contentManagementService.findEditable(pageable))
    }
}
