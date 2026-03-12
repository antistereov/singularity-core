package io.stereov.singularity.file.core.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.content.core.component.AccessCriteria
import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.database.core.exception.DeleteDocumentByKeyException
import io.stereov.singularity.database.core.exception.FindDocumentByKeyException
import io.stereov.singularity.database.core.model.DocumentKey
import io.stereov.singularity.database.core.util.CriteriaBuilder
import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.file.core.exception.FileException
import io.stereov.singularity.file.core.exception.FileMetadataException
import io.stereov.singularity.file.core.exception.GetFilesException
import io.stereov.singularity.file.core.mapper.FileMetadataMapper
import io.stereov.singularity.file.core.model.*
import io.stereov.singularity.file.core.properties.StorageProperties
import io.stereov.singularity.file.core.util.FileKeyHelper
import io.stereov.singularity.file.core.util.mediaType
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.global.util.mapContent
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.codec.multipart.FilePart
import java.time.Instant

/**
 * Abstract class representing a file storage system with support for uploading,
 * managing metadata, and retrieving file-related data.
 * Provides functionality
 * to handle file operations and maintain metadata consistency.
 */
abstract class FileStorage {

    abstract val metadataService: FileMetadataService
    abstract val appProperties: AppProperties
    abstract val logger: KLogger
    abstract val metadataMapper: FileMetadataMapper
    abstract val storageProperties: StorageProperties
    abstract val accessCriteria: AccessCriteria

    suspend fun upload(
        file: FilePart,
        filename: String = file.filename(),
        path: String?,
        isPublic: Boolean,
        authentication: AuthenticationOutcome.Authenticated,
    ): Result<FileMetadataResponse, FileException> = coroutineBinding {
        val contentType = file.mediaType().bind()

        val fileKeyHelper = FileKeyHelper(
            filename,
            file.headers().contentType,
            path
        )
        val documentKey = fileKeyHelper.toDocumentKey()
        val renditionKey = fileKeyHelper.toRenditionKey()

        val req = FileUploadRequest.FilePartUpload(
            key = renditionKey,
            mediaType = contentType,
            data = file,
        )
        metadataService.existsRenditionByKey(renditionKey)
            .mapError { ex -> when (ex) {
                is FileMetadataException.Database -> FileException.Metadata("Failed to check existence of file rendition with key '${renditionKey}': ${ex.message}", ex)
            } }
            .andThen { exists ->
                if(exists) {
                    Err(FileException.FileKeyTaken("File rendition with key '${renditionKey}' already exists"))
                } else {
                    Ok(null)
                }
            }
            .bind()
        val upload = uploadRendition(req).bind()

        val metadata = metadataService.save(FileMetadataDocument(
            id = null,
            key = documentKey,
            ownerId = authentication.principalId,
            isPublic = isPublic,
            renditions = mapOf(FileMetadataDocument.ORIGINAL_RENDITION to metadataMapper.toRendition(upload)),
        )).flatMapEither(
            { Ok(it) },
            { ex -> handleOutOfSync(renditionKey, ex) }
        ).bind()

        metadataMapper.toMetadataResponse(metadata, authentication).bind()
    }

    /**
     * Uploads multiple renditions of a file and associates them with the specified key and metadata.
     *
     * This method performs several operations, including checking the existence of each rendition,
     * uploading the renditions, saving metadata, and handling out-of-sync metadata situations.
     *
     * @param authentication The authentication token of the user performing the upload.
     * @param key A unique key identifying the file for which renditions are being uploaded.
     * @param files A map of rendition identifiers to their respective file upload requests.
     * @param isPublic Indicates whether the uploaded file renditions should be publicly accessible.
     * @return A [Result] containing the metadata response for the uploaded file and renditions on success,
     * or an appropriate [FileException] on failure.
     */
    suspend fun uploadMultipleRenditions(
        key: DocumentKey,
        files: Map<String, FileUploadRequest>,
        isPublic: Boolean,
        authentication: AuthenticationOutcome.Authenticated,
    ): Result<FileMetadataResponse, FileException> = coroutineBinding {
        files.values.forEach {
            metadataService.existsRenditionByKey(it.key)
                .mapError { ex -> FileException.Metadata("Failed to fetch metadata for rendition with key $key: ${ex.message}", ex) }
                .andThen { exists ->
                    if (exists) {
                        Err(FileException.FileKeyTaken("File with key ${it.key} already exists"))
                    } else {
                        Ok(true)
                    }
                }
                .bind()
        }

        val deferredUploads: Map<String, Deferred<FileUploadResponse>> = coroutineScope {
            files.mapValues { (_, file) ->
                async { uploadRendition(file).bind() }
            }
        }
        val uploads: Map<String, FileUploadResponse> = deferredUploads.mapValues { (_, job) ->
            job.await()
        }
        val doc = metadataService.save(FileMetadataDocument(
            id = null,
            key = key,
            ownerId = authentication.principalId,
            isPublic = isPublic,
            renditions = uploads.map { (id, upload) -> id to metadataMapper.toRendition(upload) }.toMap()
        ))
            .flatMapEither(
                success = { Ok(it) },
                failure = { ex -> uploads.map { (_, rendition) ->
                    handleOutOfSync(rendition.key, ex) }
                        .reduce { result, next -> result.mapBoth(
                            success = { next },
                            failure = { ex ->
                                if (ex is FileException.MetadataOutOfSync) {
                                    result
                                } else {
                                    next
                                }
                            }
                        ) }
                }
            ).bind()

        metadataMapper.toMetadataResponse(doc, authentication).bind()
    }

    /**
     * Handles a situation where file metadata becomes out of sync by attempting to remove a file's rendition
     * and returning an appropriate result based on the success or failure of the removal operation.
     *
     * @param key The key of the file whose metadata is out of sync.
     * @param ex An exception detailing the cause of the sync issue.
     * @return A [Result] containing either an error related to metadata synchronization or [FileException]
     * indicating a metadata failure.
     */
    private suspend fun handleOutOfSync(key: FileRenditionKey, ex: Throwable): Result<FileMetadataDocument, FileException> {
        return removeRendition(key)
            .flatMapBoth(
                success = { Err(FileException.Metadata("Failed to save metadata for file $key: ${ex.message}", ex)) },
                failure = { Err(FileException.MetadataOutOfSync("Metadata out of sync: successfully saved file with key $key but failed to store metadata - attempt to remove file failed: ${ex.message}", ex))}
            )
    }

    suspend fun exists(
        key: DocumentKey
    ): Result<Boolean, FileException> = coroutineBinding {
        logger.debug { "Checking existence of file with key '$key'" }

        val metadata = metadataService.findByKey(key)
            .recoverIf(
                { ex -> ex is FindDocumentByKeyException.NotFound },
                {
                    renditionExists(FileRenditionKey(key.value)).onSuccess {
                        removeRendition(FileRenditionKey(key.value)).onSuccess {
                            logger.warn { "No metadata found for key '$key' but a file was found and deleted to maintain consistency."}
                        }
                    }
                    return@coroutineBinding false
                }
            )
            .mapError { ex ->
                FileException.NotFound("No metadata document found for key '$key': ${ex.message}", ex)
            }
            .bind()

        metadata.renditions.values.forEach { rendition ->
            val renditionExists = renditionExists(rendition.key).bind()
            if (!renditionExists) {
                logger.warn { "No file found with key '$key' but metadata found in database. " +
                        "The metadata will be removed from the database to maintain consistency."}

                metadata.renditions.values.forEach { rendition ->
                    removeRendition(rendition.key)
                }
                metadataService.deleteByKey(key)
                return@coroutineBinding false
            }
        }
        true
    }

    suspend fun remove(key: DocumentKey): Result<Unit, FileException> = coroutineBinding {
        logger.debug { "Removing file metadata and renditions associated with key '$key'" }

        val metadata = metadataService.findByKey(key)
            .mapError { ex -> when (ex) {
                is FindDocumentByKeyException.NotFound -> FileException.NotFound("No metadata found for file with key '$key': ${ex.message}", ex)
                is FindDocumentByKeyException.Database -> FileException.Metadata("Failed to retrieve file metadata for file with key '$key': ${ex.message}", ex)
            } }
            .bind()

        metadata.renditions.values.forEach { rendition ->
            removeRendition(rendition.key).bind()
            metadataService.deleteRenditionByKey(rendition.key)
                .mapError { ex -> FileException.MetadataOutOfSync("Failed to delete metadata for rendition with key '$key': ${ex.message}", ex) }
                .bind()
        }
        metadataService.deleteByKey(key)
            .recoverIf(
                { it is DeleteDocumentByKeyException.NotFound },
                {}
            )
            .mapError { ex ->
                FileException.Metadata("Failed to delete metadata for key '$key': ${ex.message}", ex)
            }
            .bind()
    }

    suspend fun metadataResponseByKey(key: DocumentKey, authentication: AuthenticationOutcome): Result<FileMetadataResponse, FileException> {
        logger.debug { "Creating metadata response for file with key '$key'" }

        return metadataService.findByKey(key)
            .mapError { ex -> FileException.Metadata("No metadata found for file with key '$key': ${ex.message}", ex) }
            .andThen { metadata ->
                metadataMapper.toMetadataResponse(metadata, authentication)
            }
    }

    protected suspend fun resolveMetadataSyncConflicts(
        fileExists: Boolean,
        renditionKey: FileRenditionKey
    ): Result<FileMetadataDocument, FileException> {

        return metadataService.findRenditionByKey(renditionKey)
            .flatMapEither(
                success = { metadata ->
                    if (fileExists) { Ok(metadata) } else {
                        metadataService.deleteRenditionByKey(renditionKey)
                            .mapError { ex ->
                                FileException.MetadataOutOfSync(
                                    "File with key $renditionKey does not exist but metadata was found; attempt to resolve conflict failed: ${ex.message}",
                                    ex
                                )
                            }
                            .andThen {
                                Err(
                                    FileException.NotFound(
                                        "No metadata for file with key $renditionKey found; file was removed to resolve this conflict",
                                    )
                                )
                            }
                    }
                    },
                failure = { ex ->
                    when (ex) {
                        is FileMetadataException.NotFound -> {
                            if (fileExists) {
                                logger.warn { "No metadata found for rendition with key $renditionKey but file exists, removing file..." }
                                removeRendition(renditionKey)
                                    .mapError { ex ->
                                        FileException.MetadataOutOfSync(
                                            "File with key $renditionKey found but no metadata was found; attempt to resolve conflict failed: ${ex.message}",
                                            ex
                                        )
                                    }
                                    .andThen {
                                        Err(
                                            FileException.NotFound(
                                                "No metadata for file with key $renditionKey found; file was removed to resolve this conflict",
                                                ex
                                            )
                                        )
                                    }
                            } else {
                                Err(FileException.NotFound("File with key $renditionKey not found: ${ex.message}", ex))
                            }
                        }

                        is FileMetadataException.Database -> {
                            if (fileExists) {
                                Err(
                                    FileException.Metadata(
                                        "Failed to fetch metadata for file with key $renditionKey: ${ex.message}",
                                        ex
                                    )
                                )
                            } else {
                                Err(FileException.NotFound("File with key $renditionKey not found"))
                            }
                        }
                    }
                }
            )
    }

    suspend fun getFiles(
        pageable: Pageable,
        authenticationOutcome: AuthenticationOutcome,
        key: String?,
        contentTypes: List<String>,
        tags: List<String>,
        roles: Set<String>,
        createdAtBefore: Instant?,
        createdAtAfter: Instant?,
        updatedAtBefore: Instant?,
        updatedAtAfter: Instant?,
    ): Result<Page<FileMetadataResponse>, GetFilesException> = coroutineBinding {

        val criteria = CriteriaBuilder(accessCriteria.generate(roles))
            .fieldContains(FileMetadataDocument::key, key)
            .isIn(FileMetadataDocument::contentTypes, contentTypes)
            .compare(FileMetadataDocument::createdAt, createdAtBefore, createdAtAfter)
            .compare(FileMetadataDocument::updatedAt, updatedAtBefore, updatedAtAfter)
            .isIn(FileMetadataDocument::tags, tags)
            .build()

        val files = metadataService.findAllPaginated(pageable, criteria)
            .mapError { GetFilesException.from(it) }
            .bind()

        files.mapContent { file ->
            metadataResponseByKey(file.key, authenticationOutcome)
                .mapError { ex -> GetFilesException.File("Failed to retrieve file with key '${file.key}': ${ex.message}", ex) }
                .bind()
        }
    }

    suspend fun serveFile(
        renditionKey: FileRenditionKey,
        authenticationOutcome: AuthenticationOutcome
    ): Result<ServedFile, FileException> = coroutineBinding {
        logger.debug { "Serving file with rendition key '$renditionKey'" }

        if (renditionKey.value.isBlank()) {
            Err(FileException.BadRequest("Rendition key cannot be blank")).bind()
        }

        val fileExists = renditionExists(renditionKey).bind()
        val metadata = resolveMetadataSyncConflicts(fileExists, renditionKey).bind()

        metadataService.requireAuthorization(authenticationOutcome, metadata, ContentAccessRole.VIEWER)
            .mapError { ex -> FileException.from(ex) }
            .bind()

        val rendition = metadata.renditions.values.firstOrNull { it.key == renditionKey }
            .toResultOr { FileException.Metadata("Metadata does not contain rendition with key $renditionKey although it was found by this key") }
            .bind()

        doServeFile(rendition)
            .bind()
    }

    protected abstract suspend fun doServeFile(rendition: FileRendition): Result<ServedFile, FileException>
    protected abstract suspend fun uploadRendition(req: FileUploadRequest): Result<FileUploadResponse, FileException.Operation>
    protected abstract suspend fun renditionExists(key: FileRenditionKey): Result<Boolean, FileException.Operation>
    protected abstract suspend fun removeRendition(key: FileRenditionKey): Result<Unit, FileException.Operation>
}
