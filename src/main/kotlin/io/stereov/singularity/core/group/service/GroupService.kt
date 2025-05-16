package io.stereov.singularity.core.group.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.core.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.core.group.model.GroupDocument
import io.stereov.singularity.core.group.repository.GroupRepository
import org.bson.types.ObjectId
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

    suspend fun findByIdOrNull(id: ObjectId): GroupDocument? {
        logger.debug { "Fining group by ID: $id" }

        return repository.findById(id)
    }

    suspend fun findById(id: ObjectId): GroupDocument {
        return findByIdOrNull(id) ?: throw DocumentNotFoundException("No group with ID $id found")
    }
}
