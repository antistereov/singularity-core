package io.stereov.singularity.content.core.service

import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.auth.core.exception.model.NotAuthorizedException
import io.stereov.singularity.auth.core.model.AccessType
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.content.core.model.ContentDocument
import io.stereov.singularity.content.core.repository.ContentRepository
import io.stereov.singularity.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.auth.group.model.KnownGroups
import org.bson.types.ObjectId
import java.time.Instant

interface ContentService<T: ContentDocument<T>>  {

    val repository: ContentRepository<T>
    val contentClass: Class<T>
    val authorizationService: AuthorizationService

    val logger: KLogger

    suspend fun findByIdOrNull(id: ObjectId): T? {
        logger.debug { "Finding ${contentClass.simpleName} by ID \"$id\"" }

        return repository.findById(id)
    }

    suspend fun findById(id: ObjectId): T {
        return findByIdOrNull(id) ?: throw DocumentNotFoundException("No ${contentClass.simpleName} with ID \"$id\" found")
    }

    suspend fun findByKeyOrNull(key: String): T? {
        logger.debug { "Fining ${contentClass.simpleName} by key \"$key\"" }

        return repository.findByKey(key)
    }

    suspend fun findByKey(key: String): T {
        return findByKeyOrNull(key) ?: throw DocumentNotFoundException("No ${contentClass.simpleName} with key \"$key\" found")
    }

    suspend fun save(content: T): T {
        logger.debug { "Saving content ${content.id}" }

        content.updatedAt = Instant.now()

        return repository.save(content)
    }
    suspend fun deleteById(id: ObjectId) {
        logger.debug { "Deleting ${contentClass.simpleName} with id \"$id\"" }

        return repository.deleteById(id)
    }

    suspend fun deleteByKey(key: String) {
        logger.debug { "Deleting ${contentClass.simpleName} with key \"$key\"" }

        return repository.deleteByKey(key)
    }

    suspend fun deleteAll() {
        logger.debug { "Deleting all ${contentClass.simpleName}s" }

        repository.deleteAll()
    }

    suspend fun existsByKey(key: String): Boolean {
        logger.debug { "Checking if ${contentClass.simpleName} with key \"$key\" exists" }

        return repository.existsByKey(key)
    }

    /**
     * Validate that the current user belongs to the Editor group.
     *
     * @throws NotAuthorizedException if the user does not have sufficient permissions.
     */
    suspend fun requireEditorGroupMembership() {
        logger.debug { "Validate that user belongs to Editor group" }

        authorizationService.requireGroupMembership(KnownGroups.EDITOR)
    }

    suspend fun requireAuthorization(content: T, role: ContentAccessRole): T {
        logger.debug { "Validating that user has role \"$role\" in ${contentClass.simpleName} with key \"${content.key}\"" }

        val user = authorizationService.getCurrentUser()

        if (!content.hasAccess(user, role)) throw NotAuthorizedException("User does not have sufficient permission to perform this action. Required role: $role")

        return content
    }

    /**
     * Validates if the current user has sufficient permission.
     *
     * @param key The key of the content.
     * @param role The [ContentAccessRole] required to perform the action, e.g. VIEWER to view or EDIT to edit the article.
     *
     * @throws NotAuthorizedException if the user does not have sufficient permissions.
     */
    suspend fun findAuthorizedByKey(key: String, role: ContentAccessRole): T {
        logger.debug { "Finding ${contentClass.simpleName} by key \"$key\" and validating permission: $role" }

        val content = findByKey(key)

        if (role == ContentAccessRole.VIEWER && content.access.visibility == AccessType.PUBLIC) return content

        return requireAuthorization(content, role)
    }
}
