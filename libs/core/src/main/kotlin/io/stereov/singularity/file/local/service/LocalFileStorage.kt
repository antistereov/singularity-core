package io.stereov.singularity.file.local.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.model.AccessType
import io.stereov.singularity.file.core.model.FileMetadataDocument
import io.stereov.singularity.file.core.service.FileMetadataService
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.local.properties.LocalFileStorageProperties
import io.stereov.singularity.global.properties.AppProperties
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
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
) : FileStorage() {

    val apiPath = "/api/assets/"
    override val logger = KotlinLogging.logger {}

    private val baseDir: Path
        get() = Files.createDirectories(Paths.get(properties.fileDirectory))


    private suspend fun getFilePath(key: String): Path {
        return baseDir.resolve(key)
    }

    override suspend fun doUpload(
        userId: ObjectId,
        filePart: FilePart,
        key: String,
        public: Boolean,
        contentType: MediaType
    ): FileMetadataDocument {
        logger.debug { "Uploading file of content type $contentType to path \"$key\"" }

        val filePath = baseDir.resolve(key)

        Files.createDirectories(filePath.parent)
        filePart.transferTo(filePath).awaitSingleOrNull()

        val size = Files.size(filePath)

        return FileMetadataDocument(
            ownerId = userId,
            key = key,
            contentType = contentType,
            accessType = if (public) AccessType.PUBLIC else AccessType.PRIVATE,
            size = size
        )
    }

    override suspend fun doExists(key: String): Boolean {
        logger.debug { "Checking if file with path \"$key\" exists" }

        val filePath = getFilePath(key)

        return Files.exists(filePath)
    }

    override suspend fun doRemove(key: String) {
        logger.debug { "Removing local file in path \"$key\"" }

        val filePath = getFilePath(key)

        try {
            Files.deleteIfExists(filePath)
        } catch (e: Exception) {
            logger.error(e) { "Failed to delete local file with path \"$filePath\"" }
        }
    }

    override suspend fun doGetUrl(key: String): String {
        return "${appProperties.baseUrl}${apiPath}${key}"
    }
}
