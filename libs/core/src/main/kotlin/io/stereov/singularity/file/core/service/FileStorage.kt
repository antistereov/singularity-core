package io.stereov.singularity.file.core.service

import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.file.core.exception.model.FileNotFoundException
import io.stereov.singularity.file.core.mapper.FileMetadataMapper
import io.stereov.singularity.file.core.model.FileKey
import io.stereov.singularity.file.core.model.FileMetadataDocument
import io.stereov.singularity.file.core.model.FileUploadRequest
import io.stereov.singularity.file.core.model.FileUploadResponse
import io.stereov.singularity.file.core.properties.StorageProperties
import io.stereov.singularity.global.properties.AppProperties
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.bson.types.ObjectId

abstract class FileStorage {

    abstract val metadataService: FileMetadataService
    abstract val appProperties: AppProperties
    abstract val logger: KLogger
    abstract val metadataMapper: FileMetadataMapper
    abstract val storageProperties: StorageProperties

    suspend fun upload(
        key: FileKey,
        file: FileUploadRequest,
        ownerId: ObjectId,
        isPublic: Boolean,
    ): FileMetadataResponse {
        storageProperties.validateContentLength(file.contentLength)
        val upload = doUpload(file)

        val doc = metadataService.save(FileMetadataDocument(
            id = null,
            key = key.key,
            ownerId = ownerId,
            isPublic = isPublic,
            renditions = mapOf(FileMetadataDocument.ORIGINAL_RENDITION to metadataMapper.rendition(upload)),
        ))
        return createResponse(doc)
    }
    suspend fun uploadMultipleRenditions(
        ownerId: ObjectId,
        key: FileKey,
        isPublic: Boolean,
        files: Map<String, FileUploadRequest>,
    ): FileMetadataResponse {
        files.values.forEach { storageProperties.validateContentLength(it.contentLength) }

        val deferredUploads: Map<String, Deferred<FileUploadResponse>> = coroutineScope {
            files.mapValues { (_, file) ->
                async { doUpload(file) }
            }
        }
        val uploads: Map<String, FileUploadResponse> = deferredUploads.mapValues { (_, job) ->
            job.await()
        }

        val doc = metadataService.save(FileMetadataDocument(
            id = null,
            key = key.key,
            ownerId = ownerId,
            isPublic = isPublic,
            renditions = uploads.map { (id, upload) -> id to metadataMapper.rendition(upload) }.toMap()
        ))
        return createResponse(doc)
    }

    suspend fun exists(key: FileKey) = exists(key.key)
    suspend fun exists(key: String): Boolean {
        logger.debug { "Checking existence of file with key \"$key\"" }

        val existsInDb = metadataService.existsFileByKey(key)
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
    suspend fun remove(key: FileKey) = remove(key.key)
    suspend fun remove(key: String) {
        logger.debug { "Removing file with key \"$key\"" }

        metadataService.deleteFileByKey(key)
        doRemove(key)
    }

    suspend fun metadataResponseByKeyOrNull(key: String): FileMetadataResponse? {
        logger.debug { "Creating metadata response for file with key \"$key\"" }

        val doc = metadataService.findByKeyOrNull(key) ?: return null

        return createResponse(doc)
    }

    suspend fun metadataResponseByKey(key: String): FileMetadataResponse {
        return metadataResponseByKeyOrNull(key)
            ?: throw FileNotFoundException(file = null, msg = "File with key \"$key not found")
    }

    suspend fun createResponse(doc: FileMetadataDocument): FileMetadataResponse {
        return metadataMapper.metadataResponse(
            doc = doc,
            renditions = doc.renditions.map { (id, rend) ->
                id to metadataMapper.renditionResponse(rend, doGetUrl(rend.key)) }.toMap()
        )
    }

    protected abstract suspend fun doUpload(req: FileUploadRequest): FileUploadResponse
    protected abstract suspend fun doExists(key: String): Boolean
    protected abstract suspend fun doRemove(key: String)
    protected abstract suspend fun doGetUrl(key: String): String
}
