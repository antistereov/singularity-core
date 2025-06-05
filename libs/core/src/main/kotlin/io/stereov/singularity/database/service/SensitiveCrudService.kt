package io.stereov.singularity.database.service

import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.database.model.EncryptedSensitiveDocument
import io.stereov.singularity.database.model.SensitiveDocument
import io.stereov.singularity.database.repository.SensitiveCrudRepository
import io.stereov.singularity.encryption.service.EncryptionService
import io.stereov.singularity.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.secrets.service.EncryptionSecretService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import org.bson.types.ObjectId

interface SensitiveCrudService<S, D: SensitiveDocument<S>, E: EncryptedSensitiveDocument<S>> {
    val repository: SensitiveCrudRepository<E>
    val encryptionSecretService: EncryptionSecretService
    val encryptionService: EncryptionService
    val clazz: Class<S>
    val logger: KLogger


    @Suppress("UNCHECKED_CAST")
    suspend fun encrypt(document: D, otherValues: List<Any> = emptyList()): E {
        return this.encryptionService.encrypt(document) as E
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun decrypt(encrypted: E, otherValues: List<Any> = emptyList()): D {
        return encryptionService.decrypt(encrypted, otherValues, clazz) as D
    }

    suspend fun findById(id: ObjectId): D {
        logger.debug { "Finding document by ID: $id" }

        val document = this.findEncryptedById(id)

        return this.decrypt(document)
    }

    suspend fun findEncryptedById(id: ObjectId): E {
        logger.debug { "Getting encrypted document with ID: $id" }

        return repository.findById(id)
            ?: throw DocumentNotFoundException("No document found with id $id")
    }

    suspend fun findByIdOrNull(id: ObjectId): D? {
        logger.debug { "Finding document by ID: $id" }

        return this.findEncryptedByIdOrNull(id)?.let {
            this.decrypt(it)
        }
    }

    suspend fun findEncryptedByIdOrNull(id: ObjectId): E? {
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

    suspend fun deleteById(id: ObjectId) {
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

    suspend fun rotateKey() {
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
