package io.stereov.singularity.file.local.controller

import com.github.michaelbull.result.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.exception.AccessTokenExtractionException
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.content.core.exception.ContentException
import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.file.core.exception.FileException
import io.stereov.singularity.file.core.exception.FileMetadataException
import io.stereov.singularity.file.core.service.FileMetadataService
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.local.properties.LocalFileStorageProperties
import io.stereov.singularity.global.model.OpenApiConstants
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.absolute
import kotlin.io.path.exists

@Controller
@RequestMapping("/api/assets")
@ConditionalOnProperty(prefix = "singularity.file.storage", value = ["type"], havingValue = "local", matchIfMissing = true)
class LocalFileStorageController(
    private val metadataService: FileMetadataService,
    private val properties: LocalFileStorageProperties,
    private val fileStorage: FileStorage,
    private val authorizationService: AuthorizationService,
) {

    private val logger = KotlinLogging.logger {}

    @GetMapping("/**")
    @Operation(
        summary = "Get File from Local File Storage",
        description = """
            Retrieve a file from local file storage.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/roles#admins"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.ADMIN_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.ADMIN_SCOPE]),

        ]
    )
    @Throws(FileException::class, AccessTokenExtractionException::class, ContentException.NotAuthorized::class)
    suspend fun serveFile(
        exchange: ServerWebExchange
    ): ResponseEntity<Flux<DataBuffer>> {
        val fullRequestPath = exchange.request.uri.path
        val basePath = "/api/assets/"
        val key = fullRequestPath.removePrefix(basePath)

        logger.debug { "Accessing asset $key" }

        if (key.isBlank()) {
            throw FileException.BadRequest("Missing file key in request path $fullRequestPath")
        }

        val baseFileDir = runCatching { Paths.get(properties.fileDirectory).toAbsolutePath().normalize() }
            .mapError { ex -> FileException.Operation("Failed to resolve base directory ${properties.fileDirectory}: ${ex.message}", ex) }
            .getOrThrow { ex -> when (ex) {
                is FileException.Operation -> ex
            } }
        val filePath = runCatching { Paths.get(properties.fileDirectory).resolve(key).normalize().absolute() }
            .mapError { ex -> FileException.Operation("Failed to resolve file path: ${ex.message}") }
            .getOrThrow { ex -> when (ex) {
                is FileException.Operation -> ex
            } }

        val fileExists = runCatching { filePath.exists() }
            .getOrThrow { ex -> FileException.Operation("Failed to check existence of file with path $filePath: ${ex.message}", ex) }

        val metadata = metadataService.findRenditionByKey(key)
            .flatMapEither(
                success = { metadata -> Ok(metadata) },
                failure = { ex -> when (ex) {
                    is FileMetadataException.NotFound -> {
                        if (fileExists) {
                            logger.warn { "No metadata found for rendition with key $key but file exists, removing file..." }
                            fileStorage.remove(key)
                                .mapError { ex ->
                                    FileException.MetadataOutOfSync(
                                        "File with key $key found but no metadata was found; attempt to resolve conflict failed: ${ex.message}",
                                        ex
                                    )
                                }
                                .andThen { Err(FileException.Metadata("Failed to fetch metadata for file with key $key: ${ex.message}", ex)) }
                        } else {
                            Err(FileException.NotFound("File with key $key not found: ${ex.message}", ex))
                        }
                    }
                    is FileMetadataException.Database -> {
                        if (fileExists) {
                            Err(FileException.Metadata("Failed to fetch metadata for file with key $key: ${ex.message}", ex))
                        } else {
                            Err(FileException.NotFound("File with key $key not found"))
                        }
                    }
                } }
            )
            .getOrThrow { ex -> when (ex) {
                is FileException.Metadata -> ex
                is FileException.Operation -> ex
                is FileException.NotFound  -> ex
                is FileException.MetadataOutOfSync -> ex
                is FileException.UnsupportedMediaType -> ex
                is FileException.Stream -> ex
                is FileException.FileKeyTaken -> ex
                is FileException.BadRequest -> ex
            } }

        val rendition = metadata.renditions.values.firstOrNull { it.key == key }
            ?: throw FileException.Metadata("Metadata does not contain rendition with key $key although it was found by this key")

        if (!filePath.startsWith(baseFileDir)) {
            throw FileException.BadRequest( "Invalid path access: trying to access file $filePath outside of specified directory ${properties.fileDirectory}")
        }

        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { ex -> when (ex) {
                is AccessTokenExtractionException.Expired -> ex
                is AccessTokenExtractionException.Invalid -> ex
                is AccessTokenExtractionException.Cache -> ex
            } }

        metadataService.requireAuthorization(authenticationOutcome, metadata, ContentAccessRole.VIEWER)
            .getOrThrow { ex -> when (ex) {
                is ContentException.NotAuthorized -> ex
            } }

        val size = runCatching { Files.size(filePath).toString() }
            .getOrElse { ex -> throw FileException.Operation("Failed to get file size of file iwth key $key: ${ex.message}", ex) }
        val mediaType = runCatching { MediaType.parseMediaType(rendition.contentType) }
            .getOrElse { ex -> throw FileException.Metadata("Invalid media type ${rendition.contentType} saved in metadata for file with key $key: ${ex.message}", ex) }

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_LENGTH, size)
            .contentType(mediaType)
            .body(DataBufferUtils.read(filePath, DefaultDataBufferFactory(), 4096))
    }
}
