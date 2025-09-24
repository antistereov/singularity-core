package io.stereov.singularity.auth.group.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.group.dto.request.CreateGroupRequest
import io.stereov.singularity.auth.group.dto.request.UpdateGroupRequest
import io.stereov.singularity.auth.group.exception.model.GroupKeyExistsException
import io.stereov.singularity.auth.group.exception.model.InvalidGroupTranslationException
import io.stereov.singularity.auth.group.mapper.GroupMapper
import io.stereov.singularity.auth.group.model.GroupDocument
import io.stereov.singularity.auth.group.model.GroupTranslation
import io.stereov.singularity.auth.group.repository.GroupRepository
import io.stereov.singularity.translate.service.TranslatableCrudService
import io.stereov.singularity.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.user.core.model.Role
import io.stereov.singularity.user.core.service.UserService
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.reactive.collect
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Service

@Service
class GroupService(
    override val repository: GroupRepository,
    override val appProperties: AppProperties,
    private val authorizationService: AuthorizationService,
    override val reactiveMongoTemplate: ReactiveMongoTemplate,
    private val groupMapper: GroupMapper,
    private val userService: UserService
) : TranslatableCrudService<GroupTranslation, GroupDocument> {

    override val logger = KotlinLogging.logger {}
    override val collectionClazz = GroupDocument::class.java
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

        if (req.translations.isEmpty())
            throw InvalidGroupTranslationException("Failed to create group: at least one translation is needed")

        if (!req.translations.containsKey(appProperties.locale))
            throw InvalidGroupTranslationException("Failed to create group: default locale ${appProperties.locale} is not contained in translations")

        if (existsByKey(req.key)) throw GroupKeyExistsException(req.key)

        return save(groupMapper.createGroup(req))
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

    suspend fun update(key: String, req: UpdateGroupRequest): GroupDocument {
        logger.debug { "Updating group with key \"$key\"" }

        authorizationService.requireRole(Role.ADMIN)

        val group = findByKeyOrNull(key)
            ?: throw DocumentNotFoundException("No group with key \"$key\" found")

        req.translationsToDelete.forEach { locale -> group.translations.remove(locale) }
        group.translations.putAll(req.translations)

        if (!group.translations.containsKey(appProperties.locale))
            throw InvalidGroupTranslationException("Failed to update group: default locale ${appProperties.locale} is not contained in translations")

        if (key == req.key || req.key == null) return save(group)

        if (existsByKey(req.key)) throw GroupKeyExistsException("Failed to update group: a group with key ${req.key} already exists")

        group.key = req.key

        userService.findAllByGroupContaining(key)
            .asFlux()
            .map { user ->
                user.groups.remove(key)
                user.groups.add(req.key)
                user
            }
            .buffer(1000)
            .collect { users -> userService.saveAll(users) }

        return save(group)
    }

    suspend fun deleteByKey(key: String) {
        logger.debug { "Deleting group with key \"$key\"" }

        authorizationService.requireRole(Role.ADMIN)

        if (!existsByKey(key)) throw DocumentNotFoundException("No group with key \"$key\" found")

        userService.findAllByGroupContaining(key)
            .asFlux()
            .map { user ->
                user.groups.remove(key)
                user
            }
            .buffer(1000)
            .collect { users -> userService.saveAll(users) }

        return repository.deleteByKey(key)
    }
}
