package io.stereov.singularity.core.group.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.core.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.core.group.model.GroupDocument
import io.stereov.singularity.core.group.repository.GroupRepository
import org.springframework.stereotype.Service

@Service
class GroupService(
    private val repository: GroupRepository
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun save(group: GroupDocument): GroupDocument {
        logger.debug { "Saving group ${group.key}" }

        return repository.save(group)
    }

    suspend fun findByIdOrNull(id: String): GroupDocument? {
        logger.debug { "Fining group by ID: $id" }

        return repository.findById(id)
    }

    suspend fun findById(id: String): GroupDocument {
        return findByIdOrNull(id) ?: throw DocumentNotFoundException("No group with ID $id found")
    }

    suspend fun findByKeyOrNull(key: String): GroupDocument? {
        logger.debug { "Fining group by key: $key" }

        return repository.findByKey(key)
    }

    suspend fun findByKey(key: String): GroupDocument {
        return findByKeyOrNull(key) ?: throw DocumentNotFoundException("No group with key $key found")
    }

    suspend fun deleteById(id: String) {
        logger.debug { "Deleting group with ID: $id" }

        return repository.deleteById(id)
    }

    suspend fun deleteByKey(key: String) {
        logger.debug { "Deleting group by key: $key" }

        return repository.deleteByKey(key)
    }

}
