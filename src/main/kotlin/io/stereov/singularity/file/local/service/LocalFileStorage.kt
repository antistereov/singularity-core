package io.stereov.singularity.file.local.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.coroutines.runSuspendCatching
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.content.core.exception.ContentException
import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.file.core.exception.FileException
import io.stereov.singularity.file.core.exception.FileMetadataException
import io.stereov.singularity.file.core.mapper.FileMetadataMapper
import io.stereov.singularity.file.core.model.FileMetadataDocument
import io.stereov.singularity.file.core.model.FileUploadRequest
import io.stereov.singularity.file.core.model.FileUploadResponse
import io.stereov.singularity.file.core.properties.StorageProperties
import io.stereov.singularity.file.core.service.FileMetadataService
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.local.model.ServedFile
import io.stereov.singularity.file.local.properties.LocalFileStorageProperties
import io.stereov.singularity.global.properties.AppProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolute
import kotlin.io.path.exists

@Service
@Primary
@ConditionalOnProperty(prefix = "singularity.file.storage", value = ["type"], havingValue = "local", matchIfMissing = true)
class LocalFileStorage(
    private val properties: LocalFileStorageProperties,
    override val appProperties: AppProperties,
    override val metadataService: FileMetadataService,
    override val metadataMapper: FileMetadataMapper,
    override val storageProperties: StorageProperties
) : FileStorage() {

    val apiPath = "/api/assets/"
    override val logger = KotlinLogging.logger {}

    private val baseDir: Path
        get() = Files.createDirectories(Paths.get(properties.fileDirectory))


    private suspend fun getFilePath(key: String): Path {
        return baseDir.resolve(key)
    }

    suspend fun serveFile(
        authenticationOutcome: AuthenticationOutcome,
        key: String,
    ): Result<ServedFile, FileException> = coroutineBinding {

        logger.debug { "Accessing asset $key" }

        if (key.isBlank()) {
            Err(FileException.BadRequest("Missing file key in request path")).bind()
        }

        val baseFileDir = runCatching { Paths.get(properties.fileDirectory).toAbsolutePath().normalize() }
            .mapError { ex ->
                FileException.Operation(
                    "Failed to resolve base directory ${properties.fileDirectory}: ${ex.message}",
                    ex
                )
            }
            .bind()

        val filePath = runCatching { Paths.get(properties.fileDirectory).resolve(key).normalize().absolute() }
            .mapError { ex -> FileException.Operation("Failed to resolve file path: ${ex.message}") }
            .bind()

        val metadata = resolveMetadataSyncConflicts(filePath, key).bind()

        metadataService.requireAuthorization(authenticationOutcome, metadata, ContentAccessRole.VIEWER)
            .mapError { ex ->
                when (ex) {
                    is ContentException.NotAuthorized -> FileException.NotAuthorized(
                        msg = "User is not authorized to view file with key $key: ${ex.message}",
                        cause = ex
                    )
                }
            }
            .bind()

        val rendition = metadata.renditions.values.firstOrNull { it.key == key }
            .toResultOr { FileException.Metadata("Metadata does not contain rendition with key $key although it was found by this key") }
            .bind()

        if (!filePath.startsWith(baseFileDir)) {
            Err(FileException.BadRequest(
                "Invalid path access: trying to access file $filePath outside of specified directory ${properties.fileDirectory}"
            )).bind()
        }

        val size = runCatching { Files.size(filePath).toString() }
            .mapError { ex ->
                FileException.Operation(
                    "Failed to get file size of file with key $key: ${ex.message}",
                    ex
                )
            }
            .bind()
        val mediaType = runCatching { MediaType.parseMediaType(rendition.contentType) }
            .mapError { ex ->
                FileException.Metadata(
                    "Invalid media type ${rendition.contentType} saved in metadata for file with key $key: ${ex.message}",
                    ex
                )
            }
            .bind()

        ServedFile(
            mediaType = mediaType,
            size = size,
            content = DataBufferUtils.read(filePath, DefaultDataBufferFactory(), 4092),
        )
    }

    private suspend fun resolveMetadataSyncConflicts(
        filePath: Path,
        key: String
    ): Result<FileMetadataDocument, FileException> = coroutineBinding {
        val fileExists = runCatching { filePath.exists() }
            .mapError { ex ->
                FileException.Operation(
                    "Failed to check existence of file with path $filePath: ${ex.message}",
                    ex
                )
            }
            .bind()

        metadataService.findRenditionByKey(key)
            .flatMapEither(
                success = { metadata -> Ok(metadata) },
                failure = { ex ->
                    when (ex) {
                        is FileMetadataException.NotFound -> {
                            if (fileExists) {
                                logger.warn { "No metadata found for rendition with key $key but file exists, removing file..." }
                                remove(key)
                                    .mapError { ex ->
                                        FileException.MetadataOutOfSync(
                                            "File with key $key found but no metadata was found; attempt to resolve conflict failed: ${ex.message}",
                                            ex
                                        )
                                    }
                                    .andThen {
                                        Err(
                                            FileException.Metadata(
                                                "Failed to fetch metadata for file with key $key: ${ex.message}",
                                                ex
                                            )
                                        )
                                    }
                            } else {
                                Err(FileException.NotFound("File with key $key not found: ${ex.message}", ex))
                            }
                        }

                        is FileMetadataException.Database -> {
                            if (fileExists) {
                                Err(
                                    FileException.Metadata(
                                        "Failed to fetch metadata for file with key $key: ${ex.message}",
                                        ex
                                    )
                                )
                            } else {
                                Err(FileException.NotFound("File with key $key not found"))
                            }
                        }
                    }
                }
            ).bind()
    }

    override suspend fun uploadRendition(
        req: FileUploadRequest
    ): Result<FileUploadResponse, FileException.Operation> = coroutineBinding {
        logger.debug { "Uploading file of content type ${req.contentType} to path \"${req.key}\"" }

        val filePath = runCatching { baseDir.resolve(req.key.key) }
            .mapError { ex -> FileException.Operation("Failed to resolve base directory $baseDir: ${ex.message}", ex) }
            .bind()

        withContext(Dispatchers.IO) {
            runCatching { Files.createDirectories(filePath.parent) }
                .mapError { ex ->
                    FileException.Operation(
                        "Failed to create directory to safe file in ${filePath.parent}: ${ex.message}",
                        ex
                    )
                }
                .bind()
        }

        when (req) {
            is FileUploadRequest.FilePartUpload -> doUploadFilePart(filePath, req)
            is FileUploadRequest.ByteArrayUpload -> doUploadByteArray(filePath, req)
            is FileUploadRequest.DataBufferUpload -> doUploadDataBuffer(filePath, req)
        }.bind()
    }

    private suspend fun doUploadDataBuffer(
        filePath: Path,
        req: FileUploadRequest.DataBufferUpload
    ): Result<FileUploadResponse, FileException.Operation> = coroutineBinding {
        runSuspendCatching { DataBufferUtils.write(req.data, filePath).awaitSingleOrNull() }
            .mapError { ex -> FileException.Operation("Failed to save file $filePath: ${ex.message}", ex) }
            .bind()

        val size = runCatching { Files.size(filePath) }
            .mapError { ex -> FileException.Operation("Failed to read size of saved file $filePath: ${ex.message}") }
            .bind()

        FileUploadResponse(
            contentType = req.contentType,
            size = size,
            key = req.key.key,
            width = req.width,
            height = req.height
        )
    }

    private suspend fun doUploadFilePart(
        filePath: Path,
        req: FileUploadRequest.FilePartUpload
    ): Result<FileUploadResponse, FileException.Operation> = coroutineBinding {

        withContext(Dispatchers.IO) {
            runSuspendCatching { req.data.transferTo(filePath).awaitSingleOrNull() }
                .mapError { ex -> FileException.Operation("Failed to save file $filePath: ${ex.message}", ex) }
                .bind()

            val size = runCatching { Files.size(filePath) }
                .mapError { ex -> FileException.Operation("Failed to read size of saved file $filePath: ${ex.message}") }
                .bind()

            FileUploadResponse(
                contentType = req.contentType,
                size = size,
                key = req.key.key,
                width = req.width,
                height = req.height
            )
        }
    }
    private suspend fun doUploadByteArray(
        filePath: Path,
        req: FileUploadRequest.ByteArrayUpload
    ): Result<FileUploadResponse, FileException.Operation> = coroutineBinding {

        withContext(Dispatchers.IO) {
            runSuspendCatching { Files.write(filePath, req.data) }
                .mapError { ex -> FileException.Operation("Failed to save file $filePath: ${ex.message}", ex) }
                .bind()

            val size = runCatching { Files.size(filePath) }
                .mapError { ex -> FileException.Operation("Failed to read size of saved file $filePath: ${ex.message}") }
                .bind()

            FileUploadResponse(
                contentType = req.contentType,
                size = size,
                key = req.key.key,
                width = req.width,
                height = req.height
            )
        }
    }

    override suspend fun renditionExists(key: String): Result<Boolean, FileException.Operation> {
        logger.debug { "Checking if file with path \"$key\" exists" }

        val filePath = getFilePath(key)

        return runSuspendCatching {
            withContext(Dispatchers.IO) {
                return@withContext Files.exists(filePath)
            }
        }
            .mapError { ex -> FileException.Operation("Failed to check existence of local file with path $filePath: ${ex.message}", ex) }
    }

    override suspend fun removeRendition(key: String): Result<Unit, FileException.Operation> {
        logger.debug { "Removing local file in path \"$key\"" }

        val filePath = getFilePath(key)

        return runSuspendCatching {
            withContext(Dispatchers.IO) {
                Files.deleteIfExists(filePath)
            }
            Unit
        }.mapError { ex ->
            FileException.Operation("Failed to delete local file with path \"$filePath\"", ex)
        }
    }

    override suspend fun getRenditionUrl(key: String): Result<String, FileException.Operation> {
        return Ok("${appProperties.baseUri}${apiPath}${key}")
    }
}
