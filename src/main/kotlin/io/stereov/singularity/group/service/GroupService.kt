package io.stereov.singularity.group.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.group.dto.CreateGroupMultiLangRequest
import io.stereov.singularity.group.exception.model.GroupKeyExistsException
import io.stereov.singularity.group.model.GroupDocument
import io.stereov.singularity.group.repository.GroupRepository
import io.stereov.singularity.global.properties.AppProperties
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.springframework.stereotype.Service

@Service
class GroupService(
    private val repository: GroupRepository,
    private val appProperties: AppProperties
) {

    @PostConstruct
    fun initializeGroups() = runBlocking {
        logger.info { "Creating initial groups" }

        appProperties.groups.forEach { groupRequest ->
            try {
                runBlocking { create(groupRequest) }
                logger.info { "Created group with key \"${groupRequest.key}\""}
            } catch (_: GroupKeyExistsException) {
                logger.info { "Skipping creation of group with key \"${groupRequest.key}\" because it already exists"}
            }
        }
    }

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun create(req: CreateGroupMultiLangRequest): GroupDocument {
        logger.debug { "Creating group with key \"${req.key}\"" }

        if (existsByKey(req.key)) throw GroupKeyExistsException(req.key)

        return save(GroupDocument(req))
    }

    suspend fun save(group: GroupDocument): GroupDocument {
        logger.debug { "Saving group ${group.key}" }

        return repository.save(group)
    }

    suspend fun findByIdOrNull(id: ObjectId): GroupDocument? {
        logger.debug { "Fining group by ID: $id" }

        return repository.findById(id)
    }

    suspend fun existsByKey(key: String): Boolean {
        logger.debug { "Checking if group with key \"$key\" exists" }

        return repository.existsByKey(key)
    }

    suspend fun findById(id: ObjectId): GroupDocument {
        return findByIdOrNull(id) ?: throw DocumentNotFoundException("No group with ID $id found")
    }
}
