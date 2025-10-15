package io.stereov.singularity.file.core.service

import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.file.core.exception.model.FileKeyAlreadyTakenException
import io.stereov.singularity.file.core.exception.model.FileNotFoundException
import io.stereov.singularity.file.core.exception.model.UnsupportedMediaTypeException
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
import org.springframework.http.codec.multipart.FilePart

abstract class FileStorage {

    abstract val metadataService: FileMetadataService
    abstract val appProperties: AppProperties
    abstract val logger: KLogger
    abstract val metadataMapper: FileMetadataMapper
    abstract val storageProperties: StorageProperties

    suspend fun upload(
        key: FileKey,
        file: FilePart,
        ownerId: ObjectId,
        isPublic: Boolean,
    ): FileMetadataResponse {
        val contentType = file.headers().contentType?.toString()
            ?: throw UnsupportedMediaTypeException("Upload failed: no content type is specified")
        val req = FileUploadRequest.FilePartUpload(
            key = key,
            contentType = contentType,
            data = file,
        )
        if (metadataService.existsRenditionByKey(req.key.key))
            throw FileKeyAlreadyTakenException("File with key ${req.key} already exists")
        val upload = uploadRendition(req)

        val doc = metadataService.save(FileMetadataDocument(
            id = null,
            key = key.key,
            ownerId = ownerId,
            isPublic = isPublic,
            renditions = mapOf(FileMetadataDocument.ORIGINAL_RENDITION to metadataMapper.toRendition(upload)),
        ))
        return createResponse(doc)
    }

    suspend fun uploadMultipleRenditions(
        key: String,
        files: Map<String, FileUploadRequest>,
        ownerId: ObjectId,
        isPublic: Boolean,
    ): FileMetadataResponse {
        files.values.forEach {
            if (metadataService.existsRenditionByKey(it.key.key))
                throw FileKeyAlreadyTakenException("File with key ${it.key} already exists")
        }

        val deferredUploads: Map<String, Deferred<FileUploadResponse>> = coroutineScope {
            files.mapValues { (_, file) ->
                async { uploadRendition(file) }
            }
        }
        val uploads: Map<String, FileUploadResponse> = deferredUploads.mapValues { (_, job) ->
            job.await()
        }

        val doc = metadataService.save(FileMetadataDocument(
            id = null,
            key = key,
            ownerId = ownerId,
            isPublic = isPublic,
            renditions = uploads.map { (id, upload) -> id to metadataMapper.toRendition(upload) }.toMap()
        ))
        return createResponse(doc)
    }

    suspend fun exists(key: FileKey) = exists(key.key)
    suspend fun exists(key: String): Boolean {
        logger.debug { "Checking existence of file with key \"$key\"" }

        val metadata = metadataService.findByKeyOrNull(key)

        if (metadata == null) {
            logger.warn { "No metadata for file with key \"$key\" found in database but file exists. " +
                    "It will be removed now to maintain consistency." }
            removeRendition(key)
            return false
        }

        metadata.renditions.values.map {rendition ->
            val renditionExists = renditionExists(rendition.key)
            if (!renditionExists) {
                logger.warn { "No file found with key \"$key\" but metadata found in database. " +
                        "The metadata will be removed from the database to maintain consistency."}

                metadata.renditions.values.forEach { rendition ->
                    removeRendition(rendition.key)
                }
                metadataService.deleteByKey(key)
                return false
            }
        }

        return true
    }

    suspend fun remove(key: FileKey) = remove(key.key)
    suspend fun remove(key: String) {
        logger.debug { "Removing file with key \"$key\"" }

        val metadata = metadataService.findByKey(key)
        metadata.renditions.values.forEach { rendition ->
            removeRendition(rendition.key)
        }
        metadataService.deleteByKey(key)
    }

    suspend fun metadataResponseByKeyOrNull(key: String): FileMetadataResponse? {
        logger.debug { "Creating metadata response for file with key \"$key\"" }

        val doc = metadataService.findByKeyOrNull(key)
            ?: return null

        return createResponse(doc)
    }
    suspend fun metadataResponseByKey(key: String): FileMetadataResponse {
        return metadataResponseByKeyOrNull(key)
            ?: throw FileNotFoundException(file = null, msg = "File with key \"$key not found")
    }

    suspend fun createResponse(doc: FileMetadataDocument): FileMetadataResponse {
        return metadataMapper.toMetadataResponse(
            doc = doc,
            renditions = doc.renditions.map { (id, rend) ->
                id to metadataMapper.toRenditionResponse(rend, getRenditionUrl(rend.key)) }.toMap()
        )
    }

    protected abstract suspend fun uploadRendition(req: FileUploadRequest): FileUploadResponse
    protected abstract suspend fun renditionExists(key: String): Boolean
    protected abstract suspend fun removeRendition(key: String)
    protected abstract suspend fun getRenditionUrl(key: String): String
}
