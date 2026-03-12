package io.stereov.singularity.file.core.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.token.exception.AccessTokenExtractionException
import io.stereov.singularity.content.core.exception.FindContentAuthorizedException
import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.database.core.model.DocumentKey
import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.file.core.exception.FileException
import io.stereov.singularity.file.core.exception.GetFilesException
import io.stereov.singularity.file.core.model.FileRenditionKey
import io.stereov.singularity.file.core.service.FileMetadataService
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.image.service.ImageStore
import io.stereov.singularity.global.annotation.ThrowsDomainError
import io.stereov.singularity.global.model.SuccessResponse
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
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
        @RequestPart file: FilePart,
        @RequestParam filename: String?,
        @RequestParam path: String?,
        @RequestParam("public") isPublic: Boolean = false
    ): ResponseEntity<FileMetadataResponse> {
        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }

        val response = if (ImageStore.ALLOWED_MEDIA_TYPES.contains(file.headers().contentType)) {
            imageStore.upload(file, filename ?: file.filename(), path, isPublic, authenticationOutcome)
                .getOrThrow { when (it) { is FileException -> it } }
        } else {
            fileStorage.upload(file, filename ?: file.filename(), path, isPublic, authenticationOutcome)
                .getOrThrow { when (it) { is FileException -> it } }
        }

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
        @RequestParam key: DocumentKey
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

    @GetMapping("/**")
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        FindContentAuthorizedException::class,
        FileException::class
    ])
    suspend fun serveFile(
        exchange: ServerWebExchange
    ): ResponseEntity<Flux<out DataBuffer>> {
        val fullRequestPath = exchange.request.uri.path
        val basePath = "/api/files/"
        val key = FileRenditionKey(fullRequestPath.removePrefix(basePath))

        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }

        val servedFile = fileStorage.serveFile(key, authenticationOutcome)
            .getOrThrow { when (it) { is FileException -> it } }

        val mediaType = servedFile.parseMediaType()
            .getOrThrow { when (it) { is FileException.Metadata -> it } }

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_LENGTH, servedFile.size)
            .contentType(mediaType)
            .body(servedFile.content)
    }
}