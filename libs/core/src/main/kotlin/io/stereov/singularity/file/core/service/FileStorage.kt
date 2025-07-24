package io.stereov.singularity.file.core.service

import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.file.core.exception.model.FileNotFoundException
import io.stereov.singularity.file.core.model.FileMetadataDocument
import io.stereov.singularity.global.properties.AppProperties
import org.bson.types.ObjectId
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import java.util.*

abstract class FileStorage {

    abstract val metadataService: FileMetadataService
    abstract val appProperties: AppProperties
    abstract val logger: KLogger

    suspend fun upload(userId: ObjectId, filePart: FilePart, key: String, public: Boolean): FileMetadataDocument {
        val extension = filePart.filename().substringAfterLast(".", "")
        val filename = key.substringBeforeLast(".", key)

        val actualKey = if (extension.isBlank()) {
            "${appProperties.slug}/$filename-${UUID.randomUUID()}"
        } else {
            "${appProperties.slug}/$filename-${UUID.randomUUID()}.$extension"
        }

        val contentType = filePart.headers().contentType ?: MediaType.APPLICATION_OCTET_STREAM

        val document = doUpload(userId, filePart, actualKey, public, contentType)
        return metadataService.save(document)
    }
    suspend fun exists(key: String): Boolean {
        logger.debug { "Checking existence of file with key \"$key\"" }

        val existsInDb = metadataService.existsByKey(key)
        val existsAsFile = doExists(key)
        val exists = existsInDb && existsAsFile

        if (!existsInDb && existsAsFile) {
            logger.warn { "No metadata for file with key \"$key\" found in database but file exists. It will be removed now to maintain consistency." }
            remove(key)
            return false
        }

        if (!existsAsFile && existsInDb) {
            logger.warn { "No file found with key \"$key\" but metadata found in database. The metadata will be removed from the database to maintain consistency."}
            remove(key)
            return false
        }

        return exists
    }
    suspend fun remove(key: String) {
        logger.debug { "Removing file with key \"$key\"" }

        metadataService.deleteByKey(key)
        doRemove(key)
    }

    suspend fun metadataResponseByKeyOrNull(key: String): FileMetadataResponse? {
        logger.debug { "Creating metadata response for file with key \"$key\"" }

        val metadata = metadataService.findByKeyOrNull(key) ?: return null

        return metadata.toResponse()
    }

    suspend fun metadataResponseByKey(key: String): FileMetadataResponse {
        return metadataResponseByKeyOrNull(key) ?: throw FileNotFoundException(file = null, msg = "File with key \"$key not found")
    }

    private suspend fun FileMetadataDocument.toResponse(): FileMetadataResponse {
        return FileMetadataResponse(
            id = this.id,
            key = this.key,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            access = this.access,
            contentType = this.contentType.toString(),
            url = doGetUrl(this.key),
            size = this.size,
            trusted = this.trusted,
            tags = this.tags
        )
    }


    protected abstract suspend fun doUpload(userId: ObjectId, filePart: FilePart, key: String, public: Boolean, contentType: MediaType): FileMetadataDocument
    protected abstract suspend fun doExists(key: String): Boolean
    protected abstract suspend fun doRemove(key: String)
    protected abstract suspend fun doGetUrl(key: String): String
}
