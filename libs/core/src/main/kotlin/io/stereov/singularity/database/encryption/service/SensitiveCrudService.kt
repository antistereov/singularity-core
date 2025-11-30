package io.stereov.singularity.database.encryption.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.coroutines.runSuspendCatching
import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.database.core.exception.DatabaseException
import io.stereov.singularity.database.encryption.exception.*
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
import java.time.Instant

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

    private var lastSuccessfulKeyRotation: Instant? = null
    fun getLastSuccessfulKeyRotation(): Instant? = lastSuccessfulKeyRotation

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
     * If an error occurs during the query,
     * it encapsulates the failure within an [ExistsEncryptedDocumentByIdException].
     *
     * @param id The unique identifier of the document to check for existence.
     * @return A [Result] containing `true` if the document exists, `false` otherwise,
     * or an [ExistsEncryptedDocumentByIdException] if an error occurs during the operation.
     */
    @Suppress("UNUSED")
    suspend fun existsById(id: ObjectId): Result<Boolean, ExistsEncryptedDocumentByIdException> {
        logger.debug { "Checking if ${encryptedDocumentClazz.simpleName} with ID $id exists" }

        return runSuspendCatching { repository.existsById(id) }
            .mapError { ex -> ExistsEncryptedDocumentByIdException.Database("Failed to check existence of ${encryptedDocumentClazz.simpleName} with ID $id: ${ex.message}", ex) }
    }

    /**
     * Finds and decrypts a document by its ID.
     *
     * This method retrieves an encrypted document from the database using the given ID,
     * then attempts to decrypt the document. Any errors encountered during the retrieval
     * or decryption process are encapsulated in the resulting error type.
     *
     * @param id The unique identifier of the document to be retrieved and decrypted.
     * @return A [Result] containing the decrypted document on success, or an [FindEncryptedDocumentByIdException]
     * if an error occurs during the retrieval or decryption process.
     */
    suspend fun findById(id: ObjectId): Result<DecryptedDocument, FindEncryptedDocumentByIdException> {
        logger.debug { "Finding ${encryptedDocumentClazz.simpleName} by ID: $id" }

        return findEncryptedById(id)
            .mapError { when (it) {
                is FindEncryptedDocumentEncryptedByIdException.Database -> FindEncryptedDocumentByIdException.Database(it.message, it.cause)
                is FindEncryptedDocumentEncryptedByIdException.NotFound -> FindEncryptedDocumentByIdException.NotFound(it.message, it.cause)
            } }
            .andThen { encrypted ->
                decrypt(encrypted)
                    .mapError { ex -> FindEncryptedDocumentByIdException.Encryption("Failed to decrypt ${encryptedDocumentClazz.simpleName} with ID $id: ${ex.message}", ex) }
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
     * @return A [Result] containing the encrypted document on success, or an [FindEncryptedDocumentEncryptedByIdException]
     * if an error occurs during the retrieval process.
     */
    suspend fun findEncryptedById(id: ObjectId): Result<EncryptedDocument, FindEncryptedDocumentEncryptedByIdException> {
        logger.debug { "Getting encrypted ${encryptedDocumentClazz.simpleName} with ID: $id" }

        return runSuspendCatching { repository.findById(id) }
            .mapError { ex -> FindEncryptedDocumentEncryptedByIdException.Database("Failed to fetch ${encryptedDocumentClazz.simpleName} by ID $id: ${ex.message}", ex) }
            .andThen { encrypted ->
                encrypted
                    .toResultOr { FindEncryptedDocumentEncryptedByIdException.NotFound("No ${encryptedDocumentClazz.simpleName} with ID $id found") }
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
     * @return A [Result] containing the decrypted document on success, or an [SaveEncryptedDocumentException]
     * if an error occurs during the process.
     */
    suspend fun save(
        document: DecryptedDocument
    ): Result<DecryptedDocument, SaveEncryptedDocumentException> = coroutineBinding {
        logger.debug { "Saving ${encryptedDocumentClazz.simpleName} with id ${document._id}" }

        val encryptedDoc = encrypt(document)
            .mapError { ex -> SaveEncryptedDocumentException.Encryption("Failed to encrypt ${encryptedDocumentClazz.simpleName}: ${ex.message}", ex) }
            .bind()
        val savedDoc = runSuspendCatching { repository.save(encryptedDoc) }
            .mapError { ex -> SaveEncryptedDocumentException.Database("Failed to save ${encryptedDocumentClazz.simpleName}: ${ex.message}", ex) }
            .bind()

        logger.debug { "Successfully saved ${encryptedDocumentClazz.simpleName}" }

        decrypt(savedDoc)
            .mapError { ex -> SaveEncryptedDocumentException.PostCommitSideEffect("Failed to decrypt ${encryptedDocumentClazz.simpleName} after it was saved to the database successfully: ${ex.message}", ex) }
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
     * @return A [Result] containing the list of decrypted documents on success, or an [SaveAllEncryptedDocumentsException]
     * if an error occurs during the process.
     */
    suspend fun saveAll(
        documents: List<DecryptedDocument>
    ): Result<List<DecryptedDocument>, SaveAllEncryptedDocumentsException> = coroutineBinding {
        logger.debug { "Saving all ${encryptedDocumentClazz.simpleName}s" }

        val encryptedDocs = documents.map {
            encrypt(it)
                .mapError { ex -> SaveAllEncryptedDocumentsException.Encryption("Failed to encrypt ${encryptedDocumentClazz.simpleName}: ${ex.message}", ex) }
                .bind()
        }
        runSuspendCatching { repository.saveAll(encryptedDocs) }
            .mapError { ex -> SaveAllEncryptedDocumentsException.Database("Failed to save ${encryptedDocumentClazz.simpleName}s: ${ex.message}", ex) }
            .map { encrypted ->
                encrypted.map {
                    decrypt(it)
                        .mapError { ex -> SaveAllEncryptedDocumentsException.PostCommitSideEffect("Failed to decrypt ${encryptedDocumentClazz.simpleName}s after successfully saving to database: ${ex.message}", ex) }
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
     * @return A [Result] containing [Unit] on successful deletion, or an [DeleteEncryptedDocumentByIdException] if an error occurs.
     */
    open suspend fun deleteById(id: ObjectId): Result<Unit, DeleteEncryptedDocumentByIdException> {
        logger.debug { "Deleting ${encryptedDocumentClazz.simpleName} by ID $id" }

        return runSuspendCatching { repository.deleteById(id) }
            .mapError { ex -> DeleteEncryptedDocumentByIdException.Database("Failed to delete ${encryptedDocumentClazz.simpleName} by ID: ${ex.message}", ex) }
    }

    /**
     * Deletes all documents from the database.
     *
     * This method attempts to remove all documents managed by the repository. Any
     * errors encountered during this operation, including issues in the database layer,
     * will be encapsulated in the resulting error type.
     *
     * @return A [Result] containing [Unit] on successful deletion, or an [DeleteAllEncryptedDocumentsException] if an error occurs.
     */
    suspend fun deleteAll(): Result<Unit, DeleteAllEncryptedDocumentsException> {
        logger.debug { "Deleting all ${encryptedDocumentClazz.simpleName}s" }

        return runSuspendCatching { repository.deleteAll() }
            .mapError { ex -> DeleteAllEncryptedDocumentsException.Database("Failed to delete all ${encryptedDocumentClazz.simpleName}s: ${ex.message}", ex) }
    }

    /**
     * Suspends the current coroutine to find and decrypt all documents of the specified sensitive class type.
     *
     * This method retrieves a flow of encrypted documents from the repository,
     * attempts to decrypt each document,
     * and returns the result of the operation.
     * If an error occurs during the operation, it wraps the error into
     * a custom exception type for further handling.
     *
     * @return A [Result] containing either a [Flow] of decrypted documents or an exception if the operation fails.
     * The failure can be represented by a [DatabaseException.Database] or an [EncryptionException].
     */
    suspend fun findAll(): Result<Flow<Result<DecryptedDocument, EncryptionException>>, DatabaseException.Database> {
        logger.debug { "Finding all ${encryptedDocumentClazz.simpleName}s" }

        return runSuspendCatching { repository.findAll() }
            .mapError { ex -> DatabaseException.Database("Failed to find all ${encryptedDocumentClazz.simpleName}s: ${ex.message} ",  ex) }
            .map { all ->
                all.map { decrypt(it) }
        }
    }

    /**
     * Initiates the rotation of encryption secrets for all entries in the repository. This involves
     * decrypting each entry with the current secret, re-encrypting it with the newly updated secret,
     * and saving the updated entry back to the repository. If the secret key for an entry hasn't
     * changed, the rotation is skipped for that entry.
     *
     * @return A [Result] encapsulating either a successful operation as [Unit], or an
     * [RotateEncryptedDocumentSecretException] representing the reason for failure during the rotation process.
     */
    open suspend fun rotateSecret(): Result<Unit, RotateEncryptedDocumentSecretException> = coroutineBinding {
        logger.debug { "Rotating encryption secret" }

        repository.findAll()
            .map {
                val secretKey = encryptionSecretService.getCurrentSecret()
                    .mapError { ex -> RotateEncryptedDocumentSecretException.Encryption("Failed to generate current secret: ${ex.message}", ex) }
                    .bind().key
                if (it.sensitive.secretKey == secretKey) {
                    logger.debug { "Skipping rotation of document ${it._id}: Encryption secret did not change" }
                    return@map it
                }

                logger.debug { "Rotating key of document ${it._id}" }
                val decrypted = decrypt(it)
                    .mapError { ex -> RotateEncryptedDocumentSecretException.Encryption("Failed to decrypt ${encryptedDocumentClazz.simpleName}: ${ex.message}", ex) }
                    .bind()
                save(decrypted)
                    .mapError { ex -> when (ex) {
                        is SaveEncryptedDocumentException.Encryption -> RotateEncryptedDocumentSecretException.Encryption("Failed to re-encrypt ${encryptedDocumentClazz.simpleName}: ${ex.message}", ex.cause)
                        is SaveEncryptedDocumentException.Database -> RotateEncryptedDocumentSecretException.Database("Failed to save ${encryptedDocumentClazz.simpleName}: ${ex.message}", ex.cause)
                        is SaveEncryptedDocumentException.PostCommitSideEffect -> RotateEncryptedDocumentSecretException.PostCommitSideEffect("Failed to decrypt ${encryptedDocumentClazz.simpleName} after re-encrypting it successfully: ${ex.message}", ex.cause)
                    } }
            }
            .onCompletion {
                logger.debug { "Key successfully rotated" }
                lastSuccessfulKeyRotation = Instant.now()
            }
            .collect {}
    }

    /**
     * Retrieves all documents matching the specified criteria in a paginated manner.
     *
     * @param pageable the pagination information, including page number, page size, and sorting options
     * @param criteria the optional criteria used to filter the documents; defaults to null if no criteria are provided
     * @return a [Result] containing a [Page] of decrypted documents if the operation is successful,
     * or an [FindAllEncryptedDocumentsPaginatedException]
     *         in case an error occurs during database interaction or decryption
     */
    suspend fun findAllPaginated(
        pageable: Pageable,
        criteria: Criteria? = null
    ): Result<Page<DecryptedDocument>, FindAllEncryptedDocumentsPaginatedException> = coroutineBinding {
        logger.debug { "Finding ${encryptedDocumentClazz.simpleName}: page ${pageable.pageNumber}, size: ${pageable.pageSize}, sort: ${pageable.sort}" }

        val query = runCatching { criteria?.let { Query(it) } ?: Query() }
            .mapError { ex -> FindAllEncryptedDocumentsPaginatedException.Database("Failed to create query: ${ex.message}", ex) }
            .bind()

        logger.debug { "Executing count with query: $query" }

        val count = runSuspendCatching { reactiveMongoTemplate.count(query, encryptedDocumentClazz).awaitFirstOrElse { 0 } }
            .mapError { ex ->
                FindAllEncryptedDocumentsPaginatedException.Database(
                    "Failed to count ${encryptedDocumentClazz.simpleName} with given criteria: ${ex.message}",
                    ex
                )
            }
            .bind()

        val paginatedQuery = runCatching { query.with(pageable) }
            .mapError { ex ->
                FindAllEncryptedDocumentsPaginatedException.Database(
                    "Failed to create paginated query for ${encryptedDocumentClazz.simpleName}: ${ex.message}",
                    ex
                )
            }
            .bind()

        val encrypted = runSuspendCatching {
            reactiveMongoTemplate
                .find(paginatedQuery, encryptedDocumentClazz)
                .collectList()
                .awaitFirstOrElse { emptyList() }
        }
            .mapError { ex -> FindAllEncryptedDocumentsPaginatedException.Database("Failed to fetch page of ${encryptedDocumentClazz.simpleName}: ${ex.message}", ex) }
            .bind()

        val decrypted = encrypted.map {
            decrypt(it)
                .mapError { ex -> FindAllEncryptedDocumentsPaginatedException.Encryption("Failed to decrypt ${encryptedDocumentClazz.simpleName}: ${ex.message}", ex) }
                .bind()
        }

         PageImpl(decrypted, pageable, count)
    }
}
