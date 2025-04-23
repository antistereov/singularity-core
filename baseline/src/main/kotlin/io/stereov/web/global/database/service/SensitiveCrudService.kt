package io.stereov.web.global.database.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.database.model.EncryptedSensitiveDocument
import io.stereov.web.global.database.model.SensitiveDocument
import io.stereov.web.global.database.repository.SensitiveCrudRepository
import io.stereov.web.global.exception.model.DocumentNotFoundException
import io.stereov.web.global.service.encryption.service.EncryptionService
import io.stereov.web.global.service.secrets.service.EncryptionSecretService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.serialization.KSerializer

abstract class SensitiveCrudService<S, D: SensitiveDocument<S>, E: EncryptedSensitiveDocument<S>>(
    private val repository: SensitiveCrudRepository<E>,
    private val encryptionSecretService: EncryptionSecretService,
    private val encryptionService: EncryptionService,
) {

    abstract val serializer: KSerializer<S>

    abstract suspend fun encrypt(document: D, otherValues: List<Any> = emptyList()): E
    abstract suspend fun decrypt(encrypted: E, otherValues: List<Any> = emptyList()): D

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun findById(id: String): D {
        logger.debug { "Finding document by ID: $id" }

        val document = this.findEncryptedById(id)

        return this.decrypt(document)
    }

    suspend fun findEncryptedById(id: String): E {
        logger.debug { "Getting encrypted document with ID: $id" }

        return repository.findById(id)
            ?: throw DocumentNotFoundException("No document found with id $id")
    }

    suspend fun findByIdOrNull(id: String): D? {
        logger.debug { "Finding document by ID: $id" }

        return this.findEncryptedByIdOrNull(id)?.let {
            this.decrypt(it)
        }
    }

    suspend fun findEncryptedByIdOrNull(id: String): E? {
        logger.debug { "Getting encrypted document with ID: $id" }

        return repository.findById(id)
    }

    suspend fun save(document: D): D {
        this.logger.debug { "Saving document" }

        val encryptedDoc = this.encrypt(document)
        val savedDoc = this.repository.save(encryptedDoc)

        this.logger.debug { "Successfully saved user" }

        return this.decrypt(savedDoc)
    }

    suspend fun deleteById(id: String) {
        logger.debug { "Deleting document by ID $id" }

        repository.deleteById(id)
    }


    suspend fun deleteAll() {
        logger.debug { "Deleting all documents" }

        return repository.deleteAll()
    }

    suspend fun findAll(): Flow<D> {
        logger.debug { "Finding all user accounts" }

        return repository.findAll().map {
            decrypt(it)
        }
    }

    open suspend fun rotateKey() {
        logger.debug { "Rotating encryption secret" }

        this.repository.findAll()
            .map {
                if (it.sensitive.secretId == encryptionSecretService.getCurrentSecret().id) {
                    logger.debug { "Skipping rotation of document ${it._id}: Encryption secret did not change" }
                    return@map it
                }

                this.logger.debug { "Rotating key of document ${it._id}" }
                this.repository.save(this.encrypt(this.decrypt(it)))
            }
            .onCompletion { logger.debug { "Key successfully rotated" } }
            .collect {}
    }
}
