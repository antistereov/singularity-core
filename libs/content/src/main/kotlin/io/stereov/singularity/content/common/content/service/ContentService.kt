package io.stereov.singularity.content.common.content.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.content.common.content.model.ContentDocument
import io.stereov.singularity.content.common.content.repository.ContentRepository
import io.stereov.singularity.core.global.exception.model.DocumentNotFoundException
import org.bson.types.ObjectId
import java.time.Instant

abstract class ContentService<T: ContentDocument<T>>(
    private val repository: ContentRepository<T>,
    protected val contentClass: Class<T>
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun findByKeyOrNull(key: String): T? {
        logger.debug { "Fining article by key" }

        return repository.findByKey(key)
    }

    suspend fun findByKey(key: String): T {
        return findByKeyOrNull(key) ?: throw DocumentNotFoundException("No content document with key $key found")
    }

    suspend fun save(content: T): T {
        logger.debug { "Saving content ${content.id}" }

        content.updatedAt = Instant.now()

        return repository.save(content)
    }

    suspend fun existsByKey(key: String): Boolean {
        logger.debug { "Checking if ${contentClass.simpleName} with key $key exists"}

        return repository.existsByKey(key)
    }

    suspend fun deleteById(id: ObjectId) {
        logger.debug { "Deleting ${contentClass.simpleName} with id \"$id\"" }

        return repository.deleteById(id)
    }

    suspend fun deleteAll() {
        logger.debug { "Deleting all content" }

        repository.deleteAll()
    }
}
