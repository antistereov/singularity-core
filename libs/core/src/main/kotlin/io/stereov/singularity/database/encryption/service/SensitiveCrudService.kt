package io.stereov.singularity.database.encryption.service

import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.database.encryption.model.Encrypted
import io.stereov.singularity.database.encryption.model.EncryptedSensitiveDocument
import io.stereov.singularity.database.encryption.model.SensitiveDocument
import io.stereov.singularity.database.encryption.repository.SensitiveCrudRepository
import io.stereov.singularity.global.exception.model.DocumentNotFoundException
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
 * This service provides CRUD operations with encryption.
 *
 * @param SensitiveData The sensitive information contained in the document.
 * @param DecryptedDocument The decrypted document.
 * @param EncryptedDocument The encrypted document.
 */
abstract class SensitiveCrudService<SensitiveData, DecryptedDocument: SensitiveDocument<SensitiveData>, EncryptedDocument: EncryptedSensitiveDocument<SensitiveData>> {
    abstract val repository: SensitiveCrudRepository<EncryptedDocument>
    abstract val encryptionSecretService: EncryptionSecretService
    abstract val encryptionService: EncryptionService
    abstract val sensitiveClazz: Class<SensitiveData>
    abstract val encryptedDocumentClazz: Class<EncryptedDocument>
    abstract val logger: KLogger
    abstract val reactiveMongoTemplate: ReactiveMongoTemplate

    protected abstract suspend fun doEncrypt(document: DecryptedDocument, encryptedSensitive: Encrypted<SensitiveData>): EncryptedDocument

    protected abstract suspend fun doDecrypt(encrypted: EncryptedDocument, decryptedSensitive: SensitiveData): DecryptedDocument

    @Suppress("UNCHECKED_CAST")
    suspend fun encrypt(document: DecryptedDocument): EncryptedDocument {
        val wrapped = this.encryptionService.wrap(document.sensitive)
        return doEncrypt(document, wrapped)
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun decrypt(encrypted: EncryptedDocument): DecryptedDocument {
        val unwrapped = encryptionService.unwrap(encrypted.sensitive, sensitiveClazz)
        return doDecrypt(encrypted, unwrapped)
    }

    @Suppress("UNUSED")
    suspend fun existsById(id: ObjectId): Boolean {
        logger.debug { "Checking if document with ID $id exists" }

        return repository.existsById(id)
    }

    suspend fun findById(id: ObjectId): DecryptedDocument {
        logger.debug { "Finding document by ID: $id" }

        val document = this.findEncryptedById(id)

        return this.decrypt(document)
    }

    suspend fun findEncryptedById(id: ObjectId): EncryptedDocument {
        logger.debug { "Getting encrypted document with ID: $id" }

        return repository.findById(id)
            ?: throw DocumentNotFoundException("No document found with id $id")
    }

    suspend fun findByIdOrNull(id: ObjectId): DecryptedDocument? {
        logger.debug { "Finding document by ID: $id" }

        return this.findEncryptedByIdOrNull(id)?.let {
            this.decrypt(it)
        }
    }

    suspend fun findEncryptedByIdOrNull(id: ObjectId): EncryptedDocument? {
        logger.debug { "Getting encrypted document with ID: $id" }

        return repository.findById(id)
    }

    suspend fun save(document: DecryptedDocument): DecryptedDocument {
        this.logger.debug { "Saving document" }

        val encryptedDoc = this.encrypt(document)
        val savedDoc = this.repository.save(encryptedDoc)

        this.logger.debug { "Successfully saved user" }

        return this.decrypt(savedDoc)
    }

    suspend fun saveAll(documents: List<DecryptedDocument>): List<DecryptedDocument> {
        logger.debug { "Saving all documents" }

        val encryptedDocs = documents.map { encrypt(it) }
        val savedDocs = repository.saveAll(encryptedDocs)

        return savedDocs.map { decrypt(it) }
            .toList()
    }

    suspend fun deleteById(id: ObjectId) {
        logger.debug { "Deleting document by ID $id" }

        repository.deleteById(id)
    }


    suspend fun deleteAll() {
        logger.debug { "Deleting all documents" }

        return repository.deleteAll()
    }

    suspend fun findAll(): Flow<DecryptedDocument> {
        logger.debug { "Finding all user accounts" }

        return repository.findAll().map {
            decrypt(it)
        }
    }

    open suspend fun rotateSecret() {
        logger.debug { "Rotating encryption secret" }

        this.repository.findAll()
            .map {
                if (it.sensitive.secretKey == encryptionSecretService.getCurrentSecret().key) {
                    logger.debug { "Skipping rotation of document ${it._id}: Encryption secret did not change" }
                    return@map it
                }

                this.logger.debug { "Rotating key of document ${it._id}" }
                this.repository.save(this.encrypt(this.decrypt(it)))
            }
            .onCompletion { logger.debug { "Key successfully rotated" } }
            .collect {}
    }

    suspend inline fun findAllPaginated(pageable: Pageable, criteria: Criteria? = null): Page<DecryptedDocument> {
        logger.debug { "Finding ${encryptedDocumentClazz}: page ${pageable.pageNumber}, size: ${pageable.pageSize}, sort: ${pageable.sort}" }

        val query = criteria?.let { Query(it) } ?: Query()
        val paginatedQuery = query.with(pageable)

        val count = reactiveMongoTemplate.count(paginatedQuery, encryptedDocumentClazz).awaitFirstOrElse { 0 }
        val encrypted = reactiveMongoTemplate.find(paginatedQuery, encryptedDocumentClazz).collectList().awaitFirstOrElse { emptyList() }
        val decrypted = encrypted.map { decrypt(it) }

        return PageImpl(decrypted, pageable, count)
    }
}