package io.stereov.singularity.file.core.controller

import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.file.core.service.FileMetadataService
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.PageableRequest
import io.stereov.singularity.global.util.mapContent
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/content/files")
@Tag(name = "File Metadata", description = "Operations related to metadata of files")
class FileMetadataController(
    private val service: FileMetadataService,
    private val fileStorage: FileStorage
) {

    @GetMapping("{key}")
    @Operation(
        summary = "Get File Metadata By Key",
        description = """
            Get a file by given `key`.
            
            You can find more information about file management [here](https://singularity.stereov.io/docs/guides/file-storage/metadata).
            
            >**Note:** It will only return file metadata that is accessible by the requester. 
            >You can learn more about access [here](https://singularity.stereov.io/docs/guides/content/introduction#authorization-logic).
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/file-storage/metadata"),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The metadata of the file with `key`.",
            ),
            ApiResponse(
                responseCode = "404",
                description = "No file with given key exists.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
        ]
    )
    suspend fun getFileMetadataByKey(
        @PathVariable key: String
    ): ResponseEntity<FileMetadataResponse> {
        val res = fileStorage.createResponse(service.findAuthorizedByKey(key, ContentAccessRole.VIEWER))
        return ResponseEntity.ok(res)
    }

    @GetMapping
    @Operation(
        summary = "Get File Metadata",
        description = """
            Get and filter file metadata.
            
            You can find more information about file management [here](https://singularity.stereov.io/docs/guides/file-storage/metadata).
            
            >**Note:** It will only return file metadata that is accessible by the requester. 
            >You can learn more about access [here](https://singularity.stereov.io/docs/guides/content/introduction#authorization-logic).
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/file-storage/metadata"),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The metadata of the file with `key`.",
            ),
            ApiResponse(
                responseCode = "404",
                description = "No file with given key exists.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
        ]
    )
    suspend fun getFileMetadata(
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