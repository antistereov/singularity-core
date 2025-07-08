package io.stereov.singularity.file.local.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.model.AccessType
import io.stereov.singularity.content.file.model.FileDocument
import io.stereov.singularity.content.file.service.FileMetadataService
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.local.properties.LocalFileStorageProperties
import io.stereov.singularity.global.properties.AppProperties
import kotlinx.coroutines.reactor.awaitSingle
import org.bson.types.ObjectId
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Service
@ConditionalOnProperty(prefix = "singularity.file.storage", value = ["type"], havingValue = "local", matchIfMissing = true)
class LocalFileStorage(
    private val properties: LocalFileStorageProperties,
    override val appProperties: AppProperties,
    override val metadataService: FileMetadataService,
) : FileStorage() {

    val apiPath = "/api/assets/"
    override val logger = KotlinLogging.logger {}

    private fun getBaseDir(public: Boolean): Path {
        val baseDir = if (public) properties.publicPath else properties.privatePath
        return Files.createDirectories(Paths.get(baseDir))
    }

    private suspend fun getFilePath(key: String): Path {
        val isPublic = metadataService.findByKey(key).access.visibility == AccessType.PUBLIC
        return getBaseDir(isPublic).resolve(key)
    }

    override suspend fun doUpload(
        userId: ObjectId,
        filePart: FilePart,
        key: String,
        public: Boolean,
        contentType: MediaType
    ): FileDocument {
        logger.debug { "Uploading file of content type $contentType to path \"$key\"" }

        val filePath = getBaseDir(public).resolve(key)

        Files.createDirectories(filePath.parent)
        filePart.transferTo(filePath).awaitSingle()

        val size = Files.size(filePath)

        return FileDocument(
            ownerId = userId,
            key = key,
            contentType = contentType,
            accessType = if (public) AccessType.PUBLIC else AccessType.SHARED,
            publicUrl = if (public) getPublicUrl(key) else null,
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

    override suspend fun getPublicUrl(key: String): String {
        return "${appProperties.baseUrl}/${apiPath}/public/$key"
    }

    override suspend fun getPrivateUrl(key: String): String {
        return "${appProperties.baseUrl}/${apiPath}/private/$key"
    }
}
