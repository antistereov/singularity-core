package io.stereov.singularity.file.local.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.content.core.component.AccessCriteria
import io.stereov.singularity.file.core.exception.FileException
import io.stereov.singularity.file.core.mapper.FileMetadataMapper
import io.stereov.singularity.file.core.model.*
import io.stereov.singularity.file.core.properties.StorageProperties
import io.stereov.singularity.file.core.service.FileMetadataService
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.local.properties.LocalFileStorageProperties
import io.stereov.singularity.global.properties.AppProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
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
    override val storageProperties: StorageProperties,
    override val accessCriteria: AccessCriteria,
) : FileStorage() {

    override val logger = KotlinLogging.logger {}

    private val baseDir: Path
        get() = Files.createDirectories(Paths.get(properties.fileDirectory))


    private suspend fun getFilePath(key: FileRenditionKey): Path {
        return baseDir.resolve(key.value.removePrefix("/"))
    }

    override suspend fun doServeFile(
        rendition: FileRendition,
    ): Result<ServedFile, FileException> = coroutineBinding {
        logger.debug { "Accessing asset ${rendition.key}" }
        val baseFileDir = runCatching { Paths.get(properties.fileDirectory).toAbsolutePath().normalize() }
            .mapError { ex ->
                FileException.Operation(
                    "Failed to resolve base directory ${properties.fileDirectory}: ${ex.message}",
                    ex
                )
            }
            .bind()

        val filePath = runCatching { Paths.get(properties.fileDirectory).resolve(rendition.key.value).normalize().absolute() }
            .mapError { ex -> FileException.Operation("Failed to resolve file path: ${ex.message}") }
            .bind()

        runCatching { filePath.exists() }
            .mapError { ex ->
                FileException.Operation(
                    "Failed to check existence of file with path $filePath: ${ex.message}",
                    ex
                )
            }
            .bind()

        if (!filePath.startsWith(baseFileDir)) {
            Err(FileException.BadRequest(
                "Invalid path access: trying to access file $filePath outside of specified directory ${properties.fileDirectory}"
            )).bind()
        }

        val size = runCatching { Files.size(filePath).toString() }
            .mapError { ex ->
                FileException.Operation(
                    "Failed to generate file size of file with key ${rendition.key}: ${ex.message}",
                    ex
                )
            }
            .bind()

        ServedFile(
            size = size,
            content = DataBufferUtils.read(filePath, DefaultDataBufferFactory(), 4092),
            rendition = rendition
        )
    }

    override suspend fun uploadRendition(
        req: FileUploadRequest
    ): Result<FileUploadResponse, FileException.Operation> = coroutineBinding {
        logger.debug { "Uploading file of content type ${req.mediaType} to path \"${req.key}\"" }

        val filePath = getFilePath(req.key)

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
            contentType = req.mediaType.toString(),
            size = size,
            key = req.key,
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
                contentType = req.mediaType.toString(),
                size = size,
                key = req.key,
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
                contentType = req.mediaType.toString(),
                size = size,
                key = req.key,
                width = req.width,
                height = req.height
            )
        }
    }

    override suspend fun renditionExists(key: FileRenditionKey): Result<Boolean, FileException.Operation> {
        logger.debug { "Checking if file with path \"$key\" exists" }

        val filePath = getFilePath(key)

        return runSuspendCatching {
            withContext(Dispatchers.IO) {
                return@withContext Files.exists(filePath)
            }
        }
            .mapError { ex -> FileException.Operation("Failed to check existence of local file with path $filePath: ${ex.message}", ex) }
    }

    override suspend fun removeRendition(key: FileRenditionKey): Result<Unit, FileException.Operation> {
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
}
