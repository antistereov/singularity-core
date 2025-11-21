package io.stereov.singularity.file.local.service

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.file.core.exception.FileException
import io.stereov.singularity.file.core.mapper.FileMetadataMapper
import io.stereov.singularity.file.core.model.FileUploadRequest
import io.stereov.singularity.file.core.model.FileUploadResponse
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
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

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

    override suspend fun uploadRendition(
        req: FileUploadRequest
    ): Result<FileUploadResponse, FileException.Operation> {
        logger.debug { "Uploading file of content type ${req.contentType} to path \"${req.key}\"" }

        return when (req) {
            is FileUploadRequest.FilePartUpload -> doUploadFilePart(req)
            is FileUploadRequest.ByteArrayUpload -> doUploadByteArray(req)
        }
    }

    private suspend fun doUploadFilePart(
        req: FileUploadRequest.FilePartUpload
    ): Result<FileUploadResponse, FileException.Operation> = coroutineBinding {
        val filePath = baseDir.resolve(req.key.key)

        withContext(Dispatchers.IO) {
            runCatching { Files.createDirectories(filePath.parent) }
                .mapError { ex -> FileException.Operation("Failed to create directory to safe file in ${filePath.parent}: ${ex.message}", ex) }
                .bind()
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
        req: FileUploadRequest.ByteArrayUpload
    ): Result<FileUploadResponse, FileException.Operation> = coroutineBinding {
        val filePath = baseDir.resolve(req.key.key)

        withContext(Dispatchers.IO) {
            runCatching { Files.createDirectories(filePath.parent) }
                .mapError { ex -> FileException.Operation("Failed to create directory to safe file in ${filePath.parent}: ${ex.message}", ex) }
                .bind()
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
