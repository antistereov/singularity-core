package io.stereov.web.global.database.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.database.repository.SensitiveCrudRepository
import io.stereov.web.global.exception.model.DocumentNotFoundException
import io.stereov.web.global.service.encryption.model.EncryptedSensitiveDocument
import io.stereov.web.global.service.encryption.model.SensitiveDocument
import io.stereov.web.global.service.secrets.component.KeyManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import org.springframework.scheduling.annotation.Scheduled

abstract class SensitiveCrudService<S, D: SensitiveDocument<S>, E: EncryptedSensitiveDocument<S>>(
    private val repository: SensitiveCrudRepository<E>,
    private val keyManager: KeyManager,
) {

    abstract val serializer: KSerializer<S>

    abstract suspend fun encrypt(document: D, otherValues: List<Any> = emptyList()): E
    abstract suspend fun decrypt(encrypted: E, otherValues: List<Any> = emptyList()): D

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun findById(id: String): D {
        logger.debug { "Finding document by ID: $id" }

        val document = repository.findById(id)
            ?: throw DocumentNotFoundException("No document found with id $id")

        return this.decrypt(document)
    }

    suspend fun findByIdOrNull(id: String): D? {
        logger.debug { "Finding document by ID: $id" }

        return repository.findById(id)?.let {
            this.decrypt(it)
        }
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

    @Scheduled
    abstract suspend fun rotateKey(): Flow<E>
}
