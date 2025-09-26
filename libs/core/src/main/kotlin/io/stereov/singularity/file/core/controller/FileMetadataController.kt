package io.stereov.singularity.file.core.controller

import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.file.core.service.FileMetadataService
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.global.model.PageableRequest
import io.stereov.singularity.global.util.mapContent
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/content/files")
class FileMetadataController(
    private val service: FileMetadataService,
    private val fileStorage: FileStorage
) {

    @GetMapping("{key}")
    suspend fun getFileByKey(
        @PathVariable key: String
    ): ResponseEntity<FileMetadataResponse> {
        val res = fileStorage.createResponse(service.findAuthorizedByKey(key, ContentAccessRole.VIEWER))
        return ResponseEntity.ok(res)
    }

    @GetMapping
    suspend fun getFiles(
        @RequestParam page: Int = 0,
        @RequestParam size: Int = 10,
        @RequestParam sort: List<String> = emptyList(),
        @RequestParam tags: List<String> = emptyList(),
        @RequestParam roles: Set<String> = emptySet(),
        @RequestParam contentType: String?,
        @RequestParam createdAtBefore: Instant?,
        @RequestParam createdAtAfter: Instant?,
        @RequestParam updatedAtBefore: Instant?,
        @RequestParam updatedAtAfter: Instant?,
    ): ResponseEntity<Page<FileMetadataResponse>> {
        val res = service.getFiles(
            PageableRequest(page, size, sort).toPageable(),
            tags,
            roles,
            contentType,
            createdAtBefore,
            createdAtAfter,
            updatedAtBefore,
            updatedAtAfter
        )
        return ResponseEntity.ok(res.mapContent { doc -> fileStorage.createResponse(doc) })
    }
}