package io.stereov.singularity.file.core.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.token.exception.AccessTokenExtractionException
import io.stereov.singularity.content.core.exception.FindContentAuthorizedException
import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.file.core.exception.FileException
import io.stereov.singularity.file.core.exception.GetFilesException
import io.stereov.singularity.file.core.model.FileKey
import io.stereov.singularity.file.core.service.FileMetadataService
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.image.service.ImageStore
import io.stereov.singularity.global.annotation.ThrowsDomainError
import io.stereov.singularity.global.model.SuccessResponse
import org.bson.types.ObjectId
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import java.time.Instant

@RestController
@RequestMapping("/api/files")
class FileController(
    private val fileStorage: FileStorage,
    private val imageStore: ImageStore,
    private val authorizationService: AuthorizationService,
    private val fileMetadataService: FileMetadataService
) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        FileException::class
    ])
    suspend fun uploadFile(
        @RequestPart("file") file: FilePart,
        @RequestParam("path") path: String?,
        @RequestParam("filename") filename: String?,
        @RequestParam("suffix") suffix: String?,
        @RequestParam("public") public: Boolean = false
    ): ResponseEntity<FileMetadataResponse> {
        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }

        val fileKey = FileKey(filename ?: file.filename(), path, suffix)

        val response = if (ImageStore.ALLOWED_MEDIA_TYPES.contains(file.headers().contentType)) {
            imageStore.upload(authenticationOutcome, file, fileKey.key, public)
        } else {
            fileStorage.upload(authenticationOutcome, fileKey, file, public)
        }.getOrThrow { when (it) { is FileException -> it } }

        return ResponseEntity.ok(response)
    }

    @DeleteMapping
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        FindContentAuthorizedException::class,
        FileException::class
    ])
    suspend fun deleteFile(
        @RequestParam key: String
    ): ResponseEntity<SuccessResponse> {
        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }

        val file = fileMetadataService.findAuthorizedByKey(key, authenticationOutcome, ContentAccessRole.MAINTAINER)
            .getOrThrow { when (it) { is FindContentAuthorizedException -> it } }

        fileStorage.remove(file.key)
            .getOrThrow { when (it) { is FileException -> it } }

        return ResponseEntity.ok(SuccessResponse())
    }

    @GetMapping
    suspend fun findFiles(
        pageable: Pageable,
        @RequestParam key: String? = null,
        @RequestParam contentTypes: List<String> = emptyList(),
        @RequestParam tags: List<String> = emptyList(),
        @RequestParam roles: Set<String> = emptySet(),
        @RequestParam createdAtBefore: Instant?,
        @RequestParam createdAtAfter: Instant?,
        @RequestParam updatedAtBefore: Instant?,
        @RequestParam updatedAtAfter: Instant?,
    ): ResponseEntity<PagedModel<FileMetadataResponse>> {
        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }

        val response = fileStorage.getFiles(
            pageable,
            authenticationOutcome,
            key,
            contentTypes,
            tags,
            roles,
            createdAtBefore,
            createdAtAfter,
            updatedAtBefore,
            updatedAtAfter,
        ).getOrThrow { when (it) { is GetFilesException -> it } }

        return ResponseEntity.ok(PagedModel(response))
    }

    @GetMapping("/{id}")
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        FindContentAuthorizedException::class,
        FileException::class
    ])
    suspend fun serveFile(
        @PathVariable id: ObjectId,
        @RequestParam rendition: String?
    ): ResponseEntity<Flux<out DataBuffer>> {
        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }

        val fileMetadata = fileMetadataService.findAuthorizedById(id, authenticationOutcome, ContentAccessRole.VIEWER)
            .getOrThrow { when (it) { is FindContentAuthorizedException -> it } }

        val rendition = fileMetadata.getBestMatchingRendition(rendition)
            .getOrThrow { when (it) { is FileException.NotFound -> it } }

        val servedFile = fileStorage.serveFile(rendition.key, authenticationOutcome)
            .getOrThrow { when (it) { is FileException -> it } }

        val mediaType = servedFile.parseMediaType()
            .getOrThrow { when (it) { is FileException.Metadata -> it } }

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_LENGTH, servedFile.size)
            .contentType(mediaType)
            .body(servedFile.content)
    }
}