package io.stereov.singularity.file.core.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.database.core.exception.DatabaseException
import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.file.core.exception.FileException
import io.stereov.singularity.file.core.exception.FileMetadataException
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
import org.springframework.http.codec.multipart.FilePart

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

    /**
     * Uploads a file and associates it with the provided key and metadata.
     *
     * This function performs several operations, including verifying the file's content type,
     * checking if a file with the given key already exists, uploading the file rendition,
     * and saving metadata associated with the uploaded file.
     *
     * @param authentication The authentication token of the user performing the upload.
     * @param key A unique key that identifies the file to be uploaded.
     * @param file The file content to be uploaded, encapsulated in a [FilePart] object.
     * @param isPublic Indicates whether the uploaded file should be publicly accessible.
     * @return A [Result] containing the metadata response for the uploaded file on success,
     * or an appropriate [FileException] on failure.
     */
    suspend fun upload(
        authentication: AuthenticationOutcome.Authenticated,
        key: FileKey,
        file: FilePart,
        isPublic: Boolean,
    ): Result<FileMetadataResponse, FileException> = coroutineBinding {
        val contentType = file.headers().contentType?.toString()
            .toResultOr { FileException.UnsupportedMediaType("Upload failed: no content type is specified") }
            .bind()

        val req = FileUploadRequest.FilePartUpload(
            key = key,
            contentType = contentType,
            data = file,
        )
        metadataService.existsRenditionByKey(req.key.key)
            .mapError { ex -> when (ex) {
                is FileMetadataException.Database -> FileException.Metadata("Failed to check existence of file metadata with key ${req.key.key}: ${ex.message}", ex)
            } }
            .andThen { exists ->
                if(exists) {
                    Err(FileException.FileKeyTaken("File with key ${req.key} already exists"))
                } else {
                    Ok(null)
                }
            }
            .bind()
        val upload = uploadRendition(req).bind()

        val metadata = metadataService.save(FileMetadataDocument(
            id = null,
            key = key.key,
            ownerId = authentication.principalId,
            isPublic = isPublic,
            renditions = mapOf(FileMetadataDocument.ORIGINAL_RENDITION to metadataMapper.toRendition(upload)),
        )).flatMapEither(
            { Ok(it) },
            { ex -> handleOutOfSync(key.key, ex) }
        ).bind()

        createResponse(metadata, authentication).bind()

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
        authentication: AuthenticationOutcome.Authenticated,
        key: String,
        files: Map<String, FileUploadRequest>,
        isPublic: Boolean,
    ): Result<FileMetadataResponse, FileException> = coroutineBinding {
        files.values.forEach {
            metadataService.existsRenditionByKey(it.key.key)
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
                failure = { ex -> uploads.map { up ->
                    handleOutOfSync(up.key, ex) }
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

        createResponse(doc, authentication).bind()
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
    private suspend fun handleOutOfSync(key: String, ex: Throwable): Result<FileMetadataDocument, FileException> {
        return removeRendition(key)
            .flatMapBoth(
                success = { Err(FileException.Metadata("Failed to save metadata for file $key: ${ex.message}", ex)) },
                failure = { Err(FileException.MetadataOutOfSync("Metadata out of sync: successfully saved file with key $key but failed to store metadata - attempt to remove file failed: ${ex.message}", ex))}
            )
    }

    /**
     * Checks if a file associated with the provided key exists.
     *
     * This method delegates the check to another `exists` function by extracting the `key` value
     * from the provided [FileKey]. It combines the logic for checking metadata in the database
     * and verifying the existence of corresponding file renditions in storage.
     *
     * @param key The [FileKey] object representing the unique identifier of the file to check.
     * @return A [Result] containing `true` if the file exists, `false` if it does not exist, or a [FileException] in case of an error.
     */
    suspend fun exists(key: FileKey) = exists(key.key)

    /**
     * Checks if a file associated with the given key exists.
     *
     * This method verifies the existence of a file by first checking its metadata in the database.
     * If metadata is found, it ensures that the corresponding file renditions also exist in storage.
     * If the file or its renditions are missing but metadata exists, the metadata is removed to maintain consistency.
     *
     * @param key The unique identifier of the file to check.
     * @return A [Result] containing `true` if the file exists, `false` if it does not exist, or a [FileException] in case of an error.
     */
    suspend fun exists(
        key: String
    ): Result<Boolean, FileException> = coroutineBinding {
        logger.debug { "Checking existence of file with key \"$key\"" }

        val metadata = metadataService.findByKey(key)
            .recoverIf(
                { ex -> ex is DatabaseException.NotFound },
                {
                    logger.warn { "No metadata for file with key \"$key\" found in database but file exists. " +
                            "It will be removed now to maintain consistency." }
                    removeRendition(key).bind()
                    return@coroutineBinding false
                }
            )
            .mapError { ex -> FileException.Metadata("Failed to get metadata for file with key $key: ${ex.message}", ex) }
            .bind()

        metadata.renditions.values.map {rendition ->
            val renditionExists = renditionExists(rendition.key).bind()
            if (!renditionExists) {
                logger.warn { "No file found with key \"$key\" but metadata found in database. " +
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

    /**
     * Removes a file and its associated metadata by the specified key.
     *
     * This method performs the following operations:
     * - Retrieves the metadata corresponding to the provided key.
     * - Deletes all renditions associated with the file.
     * - Removes the metadata for the given key.
     *
     * Any failure during these steps results in a [FileException].
     *
     * @param key The unique key identifying the file to be removed.
     * @return A [Result] containing [Unit] if the removal is successful, or a [FileException] if an error occurs.
     */
    suspend fun remove(key: FileKey) = remove(key.key)

    /**
     * Removes a file and its associated metadata by the specified key.
     *
     * This method performs the following operations:
     * - Retrieves the metadata corresponding to the provided key.
     * - Deletes all renditions associated with the file.
     * - Removes the metadata for the given key.
     *
     * Any failure during these steps results in a [FileException].
     *
     * @param key The unique key identifying the file to be removed.
     * @return A [Result] containing [Unit] if the removal is successful, or a [FileException] if an error occurs.
     */
    suspend fun remove(key: String): Result<Unit, FileException> = coroutineBinding {
        logger.debug { "Removing file with key \"$key\"" }

        val metadata = metadataService.findByKey(key)
            .mapError { ex -> FileException.Metadata("No metadata found for file with key $key: ${ex.message}", ex) }
            .bind()

        metadata.renditions.values.forEach { rendition ->
            removeRendition(rendition.key).bind()
            metadataService.deleteRenditionByKey(rendition.key)
                .mapError { ex -> FileException.MetadataOutOfSync("Failed to delete metadata for rendition with key $key: ${ex.message}", ex) }
                .bind()
        }
        metadataService.deleteByKey(key)
            .mapError { ex ->
                FileException.Metadata("Failed to delete metadata for key $key: ${ex.message}", ex)
            }
            .bind()
    }

    /**
     * Retrieves the metadata response for a file identified by the given key.
     *
     * This function searches for the metadata associated with the specified key
     * and constructs a metadata response upon successful retrieval. In case of an
     * error, it maps and returns the corresponding [FileException].
     *
     * @param authentication The authentication token of the user making the request.
     * @param key The unique key identifying the file whose metadata is to be retrieved.
     * @return A [Result] containing the [FileMetadataResponse] on success, or a [FileException] if an error occurs.
     */
    suspend fun metadataResponseByKey(key: String, authentication: AuthenticationOutcome): Result<FileMetadataResponse, FileException> {
        logger.debug { "Creating metadata response for file with key \"$key\"" }

        return metadataService.findByKey(key)
            .mapError { ex -> FileException.Metadata("No metadata found for file with key $key: ${ex.message}", ex) }
            .andThen { metadata ->
                createResponse(metadata, authentication)
            }
    }

    /**
     * Creates a metadata response for a given file document and authentication token.
     *
     * This method maps the renditions of the provided file document to their
     * corresponding rendition responses, generates rendition URLs, and constructs
     * a comprehensive metadata response.
     *
     * @param authenticationOutcome The authentication token of the user requesting the metadata response.
     * @param doc The file metadata document containing the file's metadata and renditions.
     * @return A [Result] containing the [FileMetadataResponse] representing the metadata,
     * or a [FileException] if an error occurs during the response creation process.
     */
    suspend fun createResponse(
        doc: FileMetadataDocument,
        authenticationOutcome: AuthenticationOutcome,
    ): Result<FileMetadataResponse, FileException> = coroutineBinding {
        val renditions = doc.renditions.map { (id, rend) ->
            id to metadataMapper.toRenditionResponse(rend, getRenditionUrl(rend.key).bind())
        }.toMap()

        metadataMapper.toMetadataResponse(
            doc = doc,
            authenticationOutcome = authenticationOutcome,
            renditions = renditions
        )
            .mapError { ex -> FileException.Metadata("Failed to create file response of invalid metadata document: ${ex.message}", ex) }
            .bind()
    }

    protected suspend fun resolveMetadataSyncConflicts(
        fileExists: Boolean,
        key: String
    ): Result<FileMetadataDocument, FileException> = coroutineBinding {

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

    protected abstract suspend fun uploadRendition(req: FileUploadRequest): Result<FileUploadResponse, FileException.Operation>
    protected abstract suspend fun renditionExists(key: String): Result<Boolean, FileException.Operation>
    protected abstract suspend fun removeRendition(key: String): Result<Unit, FileException.Operation>
    protected abstract suspend fun getRenditionUrl(key: String): Result<String, FileException>
}
