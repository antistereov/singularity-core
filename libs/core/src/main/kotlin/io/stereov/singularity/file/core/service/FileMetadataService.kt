package io.stereov.singularity.file.core.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.content.core.component.AccessCriteria
import io.stereov.singularity.content.core.properties.ContentProperties
import io.stereov.singularity.content.core.service.ContentService
import io.stereov.singularity.database.core.exception.DatabaseException
import io.stereov.singularity.file.core.exception.FileMetadataException
import io.stereov.singularity.file.core.model.FileMetadataDocument
import io.stereov.singularity.file.core.repository.FileMetadataRepository
import io.stereov.singularity.database.core.util.CriteriaBuilder
import io.stereov.singularity.translate.service.TranslateService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.exists
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service

/**
 * Service for managing file metadata and renditions. Provides methods to interact with MongoDB to query,
 * check existence, delete, and save file metadata records, while adhering to access control criteria and
 * other business logic.
 *
 * This service operates on [FileMetadataDocument] objects and extends the [ContentService] class.
 * It uses a repository, an authorization service, a MongoDB template, a translation service,
 * access criteria, and content properties for its operations.
 */
@Service
class FileMetadataService(
    override val repository: FileMetadataRepository,
    override val authorizationService: AuthorizationService,
    override val reactiveMongoTemplate: ReactiveMongoTemplate,
    override val translateService: TranslateService,
    override val accessCriteria: AccessCriteria,
    override val contentProperties: ContentProperties
) : ContentService<FileMetadataDocument>() {

    override val logger = KotlinLogging.logger {}
    override val collectionClazz = FileMetadataDocument::class.java
    override val contentType = FileMetadataDocument.CONTENT_TYPE

    /**
     * Constructs a MongoDB query to find documents based on a rendition key.
     * Optionally adds access criteria if authorization is enabled.
     *
     * @param key The rendition key to search for in the database.
     * @param authorized Indicates if access criteria should be added to the query. Defaults to false.
     * @return A MongoDB query object that can be used to query the database.
     */
    private suspend fun renditionQuery(key: String, authorized: Boolean = false): Query {
        val criteria = CriteriaBuilder()
            .hasElement(FileMetadataDocument::renditionKeys, key)
        if (authorized) criteria.add(accessCriteria.getAccessCriteria())
        return criteria.query()
    }

    /**
     * Finds a file metadata document by its rendition key.
     *
     * @param key The rendition key used to identify the file metadata document in the database.
     * @return A [Result] containing either the found [FileMetadataDocument] or a [FileMetadataException]
     *         if the metadata could not be found or another error occurred.
     */
    suspend fun findRenditionByKey(key: String): Result<FileMetadataDocument, FileMetadataException> {
        logger.debug { "Finding file with key $key" }

        return runSuspendCatching {
            reactiveMongoTemplate.find<FileMetadataDocument>(renditionQuery(key))
                .awaitFirstOrNull()
        }.mapError { ex ->
            FileMetadataException.Database("Failed to get metadata with key $key: ${ex.message}", ex)
        }
            .andThen { metadata ->
                metadata.toResultOr { FileMetadataException.NotFound("No metadata for key $key found") }
            }
    }

    /**
     * Checks whether a file metadata document with the specified rendition key exists in the database.
     *
     * @param key The rendition key used to identify the file metadata document.
     * @return A [Result] containing a [Boolean] indicating whether the file metadata document exists,
     *   or a [FileMetadataException.Database] if an error occurs during the check.
     */
    suspend fun existsRenditionByKey(key: String): Result<Boolean, FileMetadataException.Database> {
        logger.debug { "Checking existence of file with key $key" }
        return runSuspendCatching {
            reactiveMongoTemplate.exists<FileMetadataDocument>(renditionQuery(key))
                .awaitFirst()
        }
            .mapError { ex -> FileMetadataException.Database("Failed to check existence of file metadata with key $key: ${ex.message}", ex) }

    }

    /**
     * Deletes a rendition identified by its key. If the rendition is the only one associated
     * with its metadata, the entire metadata document is deleted. Otherwise, the rendition
     * is removed from the metadata document.
     *
     * @param key The key identifying the rendition to delete.
     * @return A [Result] containing [Unit] if the deletion is successful, or a [FileMetadataException]
     *   in case of an error during the process.
     */
    suspend fun deleteRenditionByKey(
        key: String
    ): Result<Unit, FileMetadataException> = coroutineBinding {
        logger.debug { "Deleting rendition with key $key" }

        val metadata = findRenditionByKey(key).bind()

        if (metadata.renditionKeys.size == 1) {
            logger.debug { "Deleting metadata document because the only rendition is deleted" }
            deleteByKey(metadata.key)
                .mapError { ex -> FileMetadataException.Database("Failed to delete file metadata for key ${metadata.key}: ${ex.message}", ex) }
                .bind()
        } else {
            metadata.renditions = metadata.renditions.filter { (_,v) -> v.key != key }
            save(metadata)
                .mapError { ex -> FileMetadataException.Database("Failed to delete rendition with key $key from metadata with key ${metadata.key}: ${ex.message}", ex) }
                .bind()
        }
    }

    /**
     * Saves a [FileMetadataDocument] to the database. This method updates the `renditionKeys` and `contentTypes`
     * fields of the document based on its renditions before saving it.
     *
     * @param doc The [FileMetadataDocument] to be saved. This document contains metadata and renditions information.
     * @return A [Result] containing the saved [FileMetadataDocument] if the operation is successful, or a [DatabaseException.Database]
     * if an error occurs during the save process.
     */
    override suspend fun save(doc: FileMetadataDocument): Result<FileMetadataDocument, DatabaseException.Database> {
        doc.renditionKeys = doc.renditions.values.map { it.key }.toSet()
        doc.contentTypes = doc.renditions.values.map { it.contentType }.toSet()

        return super.save(doc)
    }
}
