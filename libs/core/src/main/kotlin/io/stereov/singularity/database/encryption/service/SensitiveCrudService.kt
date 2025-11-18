package io.stereov.singularity.database.encryption.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.database.encryption.exception.EncryptedDatabaseException
import io.stereov.singularity.database.encryption.exception.EncryptionException
import io.stereov.singularity.database.encryption.model.Encrypted
import io.stereov.singularity.database.encryption.model.EncryptedSensitiveDocument
import io.stereov.singularity.database.encryption.model.SensitiveDocument
import io.stereov.singularity.database.encryption.repository.SensitiveCrudRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirstOrElse
import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

/**
 * Abstract service for handling CRUD operations on sensitive documents with encryption and decryption mechanisms.
 *
 * This service provides functionality to store encrypted data using a repository and decrypt it where necessary for domain usage.
 * It also manages operations like encryption key rotation and pagination for large datasets.
 *
 * The main responsibilities include:
 *  - Encryption and decryption of sensitive documents using an encryption service.
 *  - Storing and fetching encrypted documents from the designated repository.
 *  - Handling operations like checking existence, saving, updating, deleting, and paginated querying of documents.
 *
 * @param SensitiveData The type of the sensitive data being handled.
 * @param DecryptedDocument The decrypted representation of the sensitive document.
 * @param EncryptedDocument The encrypted representation of the sensitive document.
 */
abstract class SensitiveCrudService<SensitiveData, DecryptedDocument: SensitiveDocument<SensitiveData>, EncryptedDocument: EncryptedSensitiveDocument<SensitiveData>> {
    abstract val repository: SensitiveCrudRepository<EncryptedDocument>
    abstract val encryptionSecretService: EncryptionSecretService
    abstract val encryptionService: EncryptionService
    abstract val sensitiveClazz: Class<SensitiveData>
    abstract val encryptedDocumentClazz: Class<EncryptedDocument>
    abstract val logger: KLogger
    abstract val reactiveMongoTemplate: ReactiveMongoTemplate

    protected abstract suspend fun doEncrypt(document: DecryptedDocument, encryptedSensitive: Encrypted<SensitiveData>): Result<EncryptedDocument, EncryptionException>

    protected abstract suspend fun doDecrypt(encrypted: EncryptedDocument, decryptedSensitive: SensitiveData): Result<DecryptedDocument, EncryptionException>

    /**
     * Encrypts a given decrypted document and returns the encrypted result.
     *
     * This method processes the sensitive data within the provided document, encrypts it,
     * and generates an encrypted document. Any encryption-specific errors encountered during
     * the operation will be encapsulated within the result.
     *
     * @param document The decrypted document containing sensitive data to be encrypted.
     * @return A [Result] containing the encrypted document on success, or an [EncryptionException]
     * if an error occurs during encryption.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun encrypt(document: DecryptedDocument): Result<EncryptedDocument, EncryptionException> {
        return encryptionService.wrap(document.sensitive).flatMap { wrapped ->
            doEncrypt(document, wrapped)
        }
    }

    /**
     * Decrypts the given encrypted document and returns its decrypted form.
     *
     * This method processes the provided encrypted document, unwraps the sensitive
     * data using the encryption service, and then performs decryption to produce a
     * decrypted document. Any errors encountered during the unwrapping or decryption
     * process will be encapsulated in the result as an [EncryptionException].
     *
     * @param encrypted The encrypted document containing sensitive data to be decrypted.
     * @return A [Result] containing the decrypted document on success, or an [EncryptionException]
     * if an error occurs during the decryption process.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun decrypt(encrypted: EncryptedDocument): Result<DecryptedDocument, EncryptionException> {
        return encryptionService.unwrap(encrypted.sensitive, sensitiveClazz).flatMap { unwrapped ->
            doDecrypt(encrypted, unwrapped)
        }
    }

    /**
     * Checks if a document with the specified ID exists in the database.
     *
     * This method verifies the existence of a document in the database using its unique identifier.
     * If an error occurs during the query, it encapsulates the failure within an [EncryptedDatabaseException.Database].
     *
     * @param id The unique identifier of the document to check for existence.
     * @return A [Result] containing `true` if the document exists, `false` otherwise, or an [EncryptedDatabaseException.Database] if an error occurs during the operation.
     */
    @Suppress("UNUSED")
    suspend fun existsById(id: ObjectId): Result<Boolean, EncryptedDatabaseException.Database> {
        logger.debug { "Checking if document with ID $id exists" }

        return runCatching { repository.existsById(id) }
            .mapError { ex -> EncryptedDatabaseException.Database("Failed to check existence of ${sensitiveClazz.simpleName} with ID $id: ${ex.message}", ex) }
    }

    /**
     * Finds and decrypts a document by its ID.
     *
     * This method retrieves an encrypted document from the database using the given ID,
     * then attempts to decrypt the document. Any errors encountered during the retrieval
     * or decryption process are encapsulated in the resulting error type.
     *
     * @param id The unique identifier of the document to be retrieved and decrypted.
     * @return A [Result] containing the decrypted document on success, or an [EncryptedDatabaseException]
     * if an error occurs during the retrieval or decryption process.
     */
    suspend fun findById(id: ObjectId): Result<DecryptedDocument, EncryptedDatabaseException> {
        logger.debug { "Finding document by ID: $id" }

        return findEncryptedById(id)
            .andThen { encrypted ->
                decrypt(encrypted)
                    .mapError { ex -> EncryptedDatabaseException.Encryption("Failed to decrypt ${sensitiveClazz.simpleName} with ID $id: ${ex.message}", ex) }
            }
    }

    /**
     * Retrieves an encrypted document by its unique identifier.
     *
     * This method fetches an encrypted document from the database using the specified ID.
     * If the document does not exist or an error occurs during retrieval, the error is encapsulated
     * in the result. The retrieval process does not decrypt the document.
     *
     * @param id The unique identifier of the encrypted document to be fetched.
     * @return A [Result] containing the encrypted document on success, or an [EncryptedDatabaseException]
     * if an error occurs during the retrieval process.
     */
    suspend fun findEncryptedById(id: ObjectId): Result<EncryptedDocument, EncryptedDatabaseException> {
        logger.debug { "Getting encrypted document with ID: $id" }

        return runCatching { repository.findById(id) }
            .mapError { ex -> EncryptedDatabaseException.Database("Failed to fetch ${sensitiveClazz.simpleName} by ID $id: ${ex.message}", ex) }
            .andThen { encrypted ->
                encrypted
                    .toResultOr { EncryptedDatabaseException.NotFound("No ${sensitiveClazz.simpleName} with ID $id found") }
            }
    }

    /**
     * Saves a decrypted document to the database after encrypting it.
     *
     * This method encrypts the provided decrypted document, stores the encrypted result in the database,
     * and then decrypts it to verify the operation. Errors that occur during encryption, database storage,
     * or post-commit decryption are encapsulated in the resulting error type.
     *
     * @param document The decrypted document to be saved.
     * @return A [Result] containing the decrypted document on success, or an [EncryptedDatabaseException]
     * if an error occurs during the process.
     */
    suspend fun save(
        document: DecryptedDocument
    ): Result<DecryptedDocument, EncryptedDatabaseException> = coroutineBinding {
        logger.debug { "Saving ${sensitiveClazz.simpleName}" }

        val encryptedDoc = encrypt(document)
            .mapError { ex -> EncryptedDatabaseException.Encryption("Failed to encrypt ${sensitiveClazz.simpleName}: ${ex.message}", ex) }
            .bind()
        val savedDoc = runCatching { repository.save(encryptedDoc) }
            .mapError { ex -> EncryptedDatabaseException.Database("Failed to save ${sensitiveClazz.simpleName}: ${ex.message}", ex) }
            .bind()

        logger.debug { "Successfully saved ${sensitiveClazz.simpleName}" }

        decrypt(savedDoc)
            .mapError { ex -> EncryptedDatabaseException.PostCommitSideEffect("Failed to decrypt ${sensitiveClazz.simpleName} after it was saved to the database successfully: ${ex.message}", ex) }
            .bind()
    }

    /**
     * Saves a list of decrypted documents to the database after encrypting them.
     *
     * This method processes each decrypted document by encrypting it, saving the resulting
     * encrypted document to the database, and then decrypting it again to verify the operation.
     * Errors encountered during encryption, database storage, or post-commit decryption are
     * encapsulated in the resulting error type.
     *
     * @param documents The list of decrypted documents to be saved.
     * @return A [Result] containing the list of decrypted documents on success, or an [EncryptedDatabaseException]
     * if an error occurs during the process.
     */
    suspend fun saveAll(
        documents: List<DecryptedDocument>
    ): Result<List<DecryptedDocument>, EncryptedDatabaseException> = coroutineBinding {
        logger.debug { "Saving all documents" }

        val encryptedDocs = documents.map {
            encrypt(it)
                .mapError { ex -> EncryptedDatabaseException.Encryption("Failed to encrypt ${sensitiveClazz.simpleName}: ${ex.message}", ex) }
                .bind()
        }
        runCatching { repository.saveAll(encryptedDocs) }
            .mapError { ex -> EncryptedDatabaseException.Database("Failed to save ${sensitiveClazz.simpleName}s: ${ex.message}", ex) }
            .map { encrypted ->
                encrypted.map {
                    decrypt(it)
                        .mapError { ex -> EncryptedDatabaseException.PostCommitSideEffect("Failed to decrypt ${sensitiveClazz.simpleName}s after successfully saving to databse: ${ex.message}", ex) }
                        .bind()
                }.toList()
            }
            .bind()

    }

    /**
     * Deletes a document by its unique identifier.
     *
     * This method attempts to remove a document from the database using the provided ID.
     * Any failure during this operation, including issues in the database layer,
     * will be encapsulated in the resulting error type.
     *
     * @param id The unique identifier of the document to delete.
     * @return A [Result] containing [Unit] on successful deletion, or an [EncryptedDatabaseException.Database] if an error occurs.
     */
    open suspend fun deleteById(id: ObjectId): Result<Unit, EncryptedDatabaseException.Database> {
        logger.debug { "Deleting document by ID $id" }

        return runCatching { repository.deleteById(id) }
            .mapError { ex -> EncryptedDatabaseException.Database("Failed to delete ${sensitiveClazz.simpleName} by ID: ${ex.message}", ex) }
    }

    /**
     * Deletes all documents from the database.
     *
     * This method attempts to remove all documents managed by the repository. Any
     * errors encountered during this operation, including issues in the database layer,
     * will be encapsulated in the resulting error type.
     *
     * @return A [Result] containing [Unit] on successful deletion, or an [EncryptedDatabaseException.Database] if an error occurs.
     */
    suspend fun deleteAll(): Result<Unit, EncryptedDatabaseException.Database> {
        logger.debug { "Deleting all documents" }

        return runCatching { repository.deleteAll() }
            .mapError { ex -> EncryptedDatabaseException.Database("Failed to delete all ${sensitiveClazz.simpleName}s: ${ex.message}", ex) }
    }

    /**
     * Retrieves and decrypts all documents managed by the repository.
     *
     * This method fetches all encrypted documents from the database and attempts to decrypt each document.
     * Any errors encountered during the decryption process are encapsulated within the resulting error type.
     *
     * @return A [Flow] emitting [Result] objects, where each result contains either a decrypted document
     *         on success or an [EncryptedDatabaseException.Encryption] in case of decryption failure.
     */
    suspend fun findAll(): Flow<Result<DecryptedDocument, EncryptedDatabaseException.Encryption>> {
        logger.debug { "Finding all user accounts" }

        return repository.findAll().map {
            decrypt(it)
                .mapError { ex -> EncryptedDatabaseException.Encryption("Failed to decrypt ${sensitiveClazz.simpleName}: ${ex.message}", ex) }
        }
    }

    /**
     * Initiates the rotation of encryption secrets for all entries in the repository. This involves
     * decrypting each entry with the current secret, re-encrypting it with the newly updated secret,
     * and saving the updated entry back to the repository. If the secret key for an entry hasn't
     * changed, the rotation is skipped for that entry.
     *
     * @return A [Result] encapsulating either a successful operation as [Unit], or an
     * [EncryptedDatabaseException] representing the reason for failure during the rotation process.
     */
    open suspend fun rotateSecret(): Result<Unit, EncryptedDatabaseException> = coroutineBinding {
        logger.debug { "Rotating encryption secret" }

        repository.findAll()
            .map {
                val secretKey = encryptionSecretService.getCurrentSecret()
                    .mapError { ex -> EncryptedDatabaseException.Encryption("Failed to get current secret: ${ex.message}", ex) }
                    .bind().key
                if (it.sensitive.secretKey == secretKey) {
                    logger.debug { "Skipping rotation of document ${it._id}: Encryption secret did not change" }
                    return@map it
                }

                logger.debug { "Rotating key of document ${it._id}" }
                val decrypted = decrypt(it)
                    .mapError { ex -> EncryptedDatabaseException.Encryption("Failed to decrypt ${sensitiveClazz.simpleName}: ${ex.message}", ex) }
                    .bind()
                val newlyEncrypted = encrypt(decrypted)
                    .mapError { ex -> EncryptedDatabaseException.Encryption("Failed to encrypt ${sensitiveClazz.simpleName}: ${ex.message}", ex) }
                    .bind()
                repository.save(newlyEncrypted)
            }
            .onCompletion { logger.debug { "Key successfully rotated" } }
            .collect {}
    }

    /**
     * Retrieves all documents matching the specified criteria in a paginated manner.
     *
     * @param pageable the pagination information, including page number, page size, and sorting options
     * @param criteria the optional criteria used to filter the documents; defaults to null if no criteria are provided
     * @return a [Result] containing a [Page] of decrypted documents if the operation is successful, or an [EncryptedDatabaseException]
     *         in case an error occurs during database interaction or decryption
     */
    suspend fun findAllPaginated(
        pageable: Pageable,
        criteria: Criteria? = null
    ): Result<Page<DecryptedDocument>, EncryptedDatabaseException> = coroutineBinding {
        logger.debug { "Finding ${encryptedDocumentClazz.simpleName}: page ${pageable.pageNumber}, size: ${pageable.pageSize}, sort: ${pageable.sort}" }

        val query = runCatching { criteria?.let { Query(it) } ?: Query() }
            .mapError { ex -> EncryptedDatabaseException.Database("Failed to create query: ${ex.message}", ex) }
            .bind()

        logger.debug { "Executing count with query: $query" }

        val count = runCatching { reactiveMongoTemplate.count(query, encryptedDocumentClazz).awaitFirstOrElse { 0 } }
            .mapError { ex ->
                EncryptedDatabaseException.Database(
                    "Failed to count ${sensitiveClazz.simpleName} with given criteria: ${ex.message}",
                    ex
                )
            }
            .bind()

        val paginatedQuery = runCatching { query.with(pageable) }
            .mapError { ex ->
                EncryptedDatabaseException.Database(
                    "Failed to create paginated query for ${sensitiveClazz.simpleName}: ${ex.message}",
                    ex
                )
            }
            .bind()

        val encrypted = runCatching {
            reactiveMongoTemplate
                .find(paginatedQuery, encryptedDocumentClazz)
                .collectList()
                .awaitFirstOrElse { emptyList() }
        }
            .mapError { ex -> EncryptedDatabaseException.Database("Failed to fetch page of ${sensitiveClazz.simpleName}: ${ex.message}", ex) }
            .bind()

        val decrypted = encrypted.map {
            decrypt(it)
                .mapError { ex -> EncryptedDatabaseException.Encryption("Failed to decrypt ${sensitiveClazz.simpleName}: ${ex.message}", ex) }
                .bind()
        }

         PageImpl(decrypted, pageable, count)
    }
}