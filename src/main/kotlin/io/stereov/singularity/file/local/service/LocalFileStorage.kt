package io.stereov.singularity.file.local.service

import io.github.oshai.kotlinlogging.KotlinLogging
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
    ): FileUploadResponse {
        logger.debug { "Uploading file of content type ${req.contentType} to path \"${req.key}\"" }

        return when (req) {
            is FileUploadRequest.FilePartUpload -> doUploadFilePart(req)
            is FileUploadRequest.ByteArrayUpload -> doUploadByteArray(req)
        }
    }

    private suspend fun doUploadFilePart(req: FileUploadRequest.FilePartUpload): FileUploadResponse {
        val filePath = baseDir.resolve(req.key.key)

        return withContext(Dispatchers.IO) {
            Files.createDirectories(filePath.parent)
            req.data.transferTo(filePath).awaitSingleOrNull()
            val size = Files.size(filePath)

            FileUploadResponse(
                contentType = req.contentType,
                size = size,
                key = req.key.key,
                width = req.width,
                height = req.height
            )
        }
    }
    private suspend fun doUploadByteArray(req: FileUploadRequest.ByteArrayUpload): FileUploadResponse {
        val filePath = baseDir.resolve(req.key.key)

        return withContext(Dispatchers.IO) {
            Files.createDirectories(filePath.parent)
            Files.write(filePath, req.data)
            val size = Files.size(filePath)

            FileUploadResponse(
                contentType = req.contentType,
                size = size,
                key = req.key.key,
                width = req.width,
                height = req.height
            )
        }
    }

    override suspend fun renditionExists(key: String): Boolean {
        logger.debug { "Checking if file with path \"$key\" exists" }

        val filePath = getFilePath(key)

        return Files.exists(filePath)
    }

    override suspend fun removeRendition(key: String) {
        logger.debug { "Removing local file in path \"$key\"" }

        val filePath = getFilePath(key)

        try {
            Files.deleteIfExists(filePath)
        } catch (e: Exception) {
            logger.error(e) { "Failed to delete local file with path \"$filePath\"" }
        }
    }

    override suspend fun getRenditionUrl(key: String): String {
        return "${appProperties.baseUri}${apiPath}${key}"
    }
}
