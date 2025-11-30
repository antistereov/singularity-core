package io.stereov.singularity.database.core.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.coroutines.runSuspendCatching
import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.database.core.exception.*
import io.stereov.singularity.database.core.model.WithId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirstOrElse
import org.bson.types.ObjectId
import org.springframework.data.domain.*
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * Interface representing a generic CRUD service for managing entities of type [T].
 *
 * It provides methods for common operations such as finding, checking existence,
 * deleting, saving, and paginating entities in a database.
 *
 * @param T The type of the entity managed by this service. It must be a non-simple class.
 */
interface CrudService<T: WithId> {

    val logger: KLogger

    /**
     * The runtime class of the entity type [T] managed by the service.
     *
     * This property is used to collect information about the entity type
     * being processed, such as its name or structure. It is commonly used
     * in logging, dynamic type handling, and operation execution related
     * to the underlying database collection.
     */
    val collectionClazz: Class<T>
    val reactiveMongoTemplate: ReactiveMongoTemplate

    /**
     * A coroutine-based repository providing CRUD operations for entities of type [T]
     * identified by [ObjectId]. It acts as a direct interface to the database
     * to handle creation, reading, updating, and deletion of entities.
     */
    val repository: CoroutineCrudRepository<T, ObjectId>

    /**
     * Finds an entity by its unique identifier.
     *
     * @param id The unique identifier of the entity to retrieve.
     * @return A [Result] containing the found entity or a [FindDocumentByIdException] if the entity
     *         is not found or an error occurs during the operation.
     */
    suspend fun findById(id: ObjectId): Result<T, FindDocumentByIdException> {
        logger.debug { "Finding ${collectionClazz.simpleName} by ID $id" }

        return runSuspendCatching { repository.findById(id) }
            .mapError { ex -> FindDocumentByIdException.Database("Failed to fetch ${collectionClazz.simpleName} with ID $id: ${ex.message}", ex) }
            .andThen { entity -> entity.toResultOr { FindDocumentByIdException.NotFound("No ${collectionClazz.simpleName} with ID $id found") } }
    }

    /**
     * Checks if a document exists in the database by its unique identifier.
     *
     * @param id The unique identifier of the document to check.
     * @return A [Result] containing `true` if the document exists, `false` if it does not,
     *   or a [ExistsDocumentByIdException] if an error occurs during the operation.
     */
    suspend fun existsById(id: ObjectId): Result<Boolean, ExistsDocumentByIdException> {
        logger.debug { "Checking if ${collectionClazz.simpleName} exists by ID $id" }

        return runSuspendCatching { repository.existsById(id) }
            .mapError { ex -> ExistsDocumentByIdException.Database("Failed to check existence of ${collectionClazz.simpleName} with ID $id: ${ex.message}", ex) }
    }

    /**
     * Deletes a document by its unique identifier.
     *
     * @param id The unique identifier of the document to delete.
     * @return A [Result] that wraps [Unit] if the deletion is successful, or a [DeleteDocumentByIdException]
     *         if an error occurs during the deletion process.
     */
    suspend fun deleteById(id: ObjectId): Result<Unit, DeleteDocumentByIdException> {
        logger.debug { "Deleting ${collectionClazz.simpleName} by ID $id" }

        return runSuspendCatching { repository.deleteById(id) }
            .mapError { ex -> DeleteDocumentByIdException.Database("Failed to delete ${collectionClazz.simpleName} with ID $id: ${ex.message}", ex) }
    }

    /**
     * Deletes all documents from the collection represented by the specified class type.
     *
     * @return A [Result] wrapping [Unit] if the operation succeeds, or a [DeleteAllDocumentsException]
     *         if an error occurs during the deletion process.
     */
    suspend fun deleteAll(): Result<Unit, DeleteAllDocumentsException> {
        logger.debug { "Deleting all ${collectionClazz.simpleName}" }

        return runSuspendCatching { repository.deleteAll() }
            .mapError { ex -> DeleteAllDocumentsException.Database("Failed to delete all ${collectionClazz.simpleName}s: ${ex.message}", ex) }
    }

    /**
     * Saves the provided document to the database.
     *
     * @param doc The document to be saved.
     * @return A [Result] containing the saved document or a [SaveDocumentException] if an error occurs during the save operation.
     */
    suspend fun save(doc: T): Result<T, SaveDocumentException> {
        logger.debug { "Saving ${collectionClazz.simpleName}" }

        return runSuspendCatching { repository.save(doc) }
            .mapError { ex -> SaveDocumentException.Database("Failed to save ${collectionClazz.name}: ${ex.message}", ex) }
    }

    /**
     * Saves the provided collection of documents to the database.
     *
     * @param docs The collection of documents to be saved.
     * @return A [Result] containing a list of saved documents or a [SaveAllDocumentsException] in case of an error.
     */
    @Suppress("UNUSED")
    suspend fun saveAll(docs: Collection<T>): Result<List<T>, SaveAllDocumentsException> {
        logger.debug { "Saving multiple ${collectionClazz.simpleName}s" }

        return runSuspendCatching { repository.saveAll(docs).toList() }
            .mapError { ex -> SaveAllDocumentsException.Database("Failed to save multiple ${collectionClazz.simpleName}s: ${ex.message}", ex) }
    }

    /**
     * Retrieves all available documents of type [T] from the database as a [Flow].
     *
     * @return A [Result] containing a [Flow] of documents of type [T] if the operation is successful,
     * or a [FindAllDocumentsException] if an error occurs during the retrieval process.
     */
    suspend fun findAll(): Result<Flow<T>, FindAllDocumentsException>  {
        return runSuspendCatching { repository.findAll() }
            .mapError { ex -> FindAllDocumentsException.Database("Failed to fetch all ${collectionClazz.simpleName}s: ${ex.message}", ex) }
    }

    /**
     * Retrieves a paginated list of elements of type [T].
     *
     * @param page The page number to retrieve, starting from 0.
     * @param size The size of the page to retrieve.
     * @param sort A list of sorting parameters in the "property, direction" format (e.g., "name,asc").
     * @param criteria Optional filter criteria for querying the database.
     *
     * @return A [Result] object containing a [Page] of elements or a [FindAllDocumentsPaginatedException] in case of an error.
     */
    suspend fun findAllPaginated(
        page: Int,
        size: Int,
        sort: List<String>,
        criteria: Criteria? = null
    ): Result<Page<T>, FindAllDocumentsPaginatedException> = coroutineBinding {

        val pageable = runCatching {
            PageRequest.of(page, size, Sort.by(sort.map { item ->
                val (property, direction) = item.split(",")
                Sort.Order(Sort.Direction.fromString(direction), property)
            }))
        }.mapError { ex -> FindAllDocumentsPaginatedException.Database("Failed to create page request: ${ex.message}", ex) }
            .bind()

        findAllPaginated(pageable, criteria).bind()
    }

    /**
     * Retrieves a paginated list of elements of type [T] based on the provided pageable parameters and optional criteria.
     *
     * @param pageable The pageable object containing page number, size, and sorting information.
     * @param criteria Optional filter criteria for querying the database.
     * @return A [Result] containing a [Page] of elements or a [FindAllDocumentsPaginatedException] in case of an error.
     */
    suspend fun findAllPaginated(
        pageable: Pageable,
        criteria: Criteria? = null
    ): Result<Page<T>, FindAllDocumentsPaginatedException> = coroutineBinding {
        logger.debug { "Finding ${collectionClazz.simpleName}: page ${pageable.pageNumber}, size: ${pageable.pageSize}, sort: ${pageable.sort}" }

        val query = runCatching { criteria?.let { Query(it) } ?: Query() }
            .mapError { ex -> FindAllDocumentsPaginatedException.Database("Failed to create query: ${ex.message}", ex) }
            .bind()
        val count = runSuspendCatching { reactiveMongoTemplate.count(query, collectionClazz).awaitFirstOrElse { 0 } }
            .mapError { ex -> FindAllDocumentsPaginatedException.Database("Failed to count ${collectionClazz.simpleName}s: ${ex.message}", ex) }
            .bind()
        val paginatedQuery = runCatching { query.with(pageable) }
            .mapError { ex -> FindAllDocumentsPaginatedException.Database("Failed to create pageable query: ${ex.message}", ex) }
            .bind()
        val groups = runSuspendCatching {
            reactiveMongoTemplate
                .find(paginatedQuery, collectionClazz)
                .collectList()
                .awaitFirstOrElse { emptyList() }
        }
            .mapError { ex -> FindAllDocumentsPaginatedException.Database("Failed to fetch page of ${collectionClazz.simpleName}s: ${ex.message}", ex) }
            .bind()

        PageImpl(groups, pageable, count)
    }
}
