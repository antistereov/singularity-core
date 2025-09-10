package io.stereov.singularity.auth.group.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.group.dto.request.CreateGroupRequest
import io.stereov.singularity.auth.group.dto.response.UpdateGroupRequest
import io.stereov.singularity.auth.group.exception.model.GroupKeyExistsException
import io.stereov.singularity.auth.group.model.GroupDocument
import io.stereov.singularity.auth.group.model.GroupTranslation
import io.stereov.singularity.auth.group.repository.GroupRepository
import io.stereov.singularity.database.core.service.TranslatableCrudService
import io.stereov.singularity.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.user.core.model.Role
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Service

@Service
class GroupService(
    private val repository: GroupRepository,
    private val appProperties: AppProperties,
    private val authorizationService: AuthorizationService,
    override val reactiveMongoTemplate: ReactiveMongoTemplate
) : TranslatableCrudService<GroupTranslation, GroupDocument> {

    override val logger = KotlinLogging.logger {}
    override val collectionClass = GroupDocument::class.java
    override val contentClass = GroupTranslation::class.java

    @PostConstruct
    fun initializeGroups() = runBlocking {
        logger.info { "Creating initial groups" }

        appProperties.groups.forEach { groupRequest ->
            try {
                runBlocking { createNotAuthorized(groupRequest) }
                logger.info { "Created group with key \"${groupRequest.key}\""}
            } catch (_: GroupKeyExistsException) {
                logger.info { "Skipping creation of group with key \"${groupRequest.key}\" because it already exists"}
            }
        }
    }

    suspend fun create(req: CreateGroupRequest): GroupDocument {
        authorizationService.requireRole(Role.ADMIN)

        return createNotAuthorized(req)
    }

    private suspend fun createNotAuthorized(req: CreateGroupRequest): GroupDocument {
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

    suspend fun findByKey(key: String): GroupDocument {
        logger.debug { "Finding group by key \"$key\"" }

        return findByKeyOrNull(key) ?: throw DocumentNotFoundException("No group with key \"$key\" found")
    }

    suspend fun findByKeyOrNull(key: String): GroupDocument? {
        logger.debug { "Finding group by key \"$key\"" }

        return repository.findByKey(key)
    }

    suspend fun existsByKey(key: String): Boolean {
        logger.debug { "Checking if group with key \"$key\" exists" }

        return repository.existsByKey(key)
    }

    suspend fun findById(id: ObjectId): GroupDocument {
        return findByIdOrNull(id) ?: throw DocumentNotFoundException("No group with ID $id found")
    }

    suspend fun update(key: String, req: UpdateGroupRequest): GroupDocument {
        logger.debug { "Updating group with key \"$key\"" }

        authorizationService.requireRole(Role.ADMIN)

        val group = findByKeyOrNull(key)
            ?: throw DocumentNotFoundException("No group with key \"$key\" found")

        group.translations.putAll(req.translations)

        return save(group)
    }

    suspend fun deleteByKey(key: String) {
        logger.debug { "Deleting group with key \"$key\"" }

        authorizationService.requireRole(Role.ADMIN)

        return repository.deleteByKey(key)
    }
}
