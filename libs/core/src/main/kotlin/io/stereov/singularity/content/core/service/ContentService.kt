package io.stereov.singularity.content.core.service

import io.stereov.singularity.auth.core.exception.model.NotAuthorizedException
import io.stereov.singularity.auth.core.model.token.AccessType
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.group.model.KnownGroups
import io.stereov.singularity.content.core.component.AccessCriteria
import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.content.core.model.ContentDocument
import io.stereov.singularity.content.core.properties.ContentProperties
import io.stereov.singularity.content.core.repository.ContentRepository
import io.stereov.singularity.database.core.service.CrudService
import io.stereov.singularity.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.translate.service.TranslateService
import java.net.URI
import java.net.URL
import java.time.Instant

abstract class ContentService<T: ContentDocument<T>> : CrudService<T>  {

    abstract override val repository: ContentRepository<T>
    abstract val authorizationService: AuthorizationService
    abstract val translateService: TranslateService
    abstract val accessCriteria: AccessCriteria
    abstract val contentProperties: ContentProperties
    abstract val contentType: String

    fun getUri(key: String): URL = URI.create(
        contentProperties.contentUri
            .replace("{contentType}", contentType)
            .replace("{contentKey}", key)
    ).toURL()

    open suspend fun findByKeyOrNull(key: String): T? {
        logger.debug { "Finding ${collectionClazz.simpleName} by key \"$key\"" }

        return repository.findByKey(key)
    }

    suspend fun findByKey(key: String): T {
        return findByKeyOrNull(key) ?: throw DocumentNotFoundException("No ${collectionClazz.simpleName} with key \"$key\" found")
    }

    open suspend fun deleteByKey(key: String) {
        logger.debug { "Deleting ${collectionClazz.simpleName} with key \"$key\"" }

        return repository.deleteByKey(key)
    }

    open suspend fun existsByKey(key: String): Boolean {
        logger.debug { "Checking if ${collectionClazz.simpleName} with key \"$key\" exists" }

        return repository.existsByKey(key)
    }

    /**
     * Validate that the current user belongs to the Editor group.
     *
     * @throws NotAuthorizedException if the user does not have sufficient permissions.
     */
    suspend fun requireContributerGroupMembership() {
        logger.debug { "Validate that user belongs to Editor group" }

        authorizationService.requireGroupMembership(KnownGroups.CONTRIBUTOR)
    }

    suspend fun requireAuthorization(content: T, role: ContentAccessRole): T {
        logger.debug { "Validating that user has role \"$role\" in ${collectionClazz.simpleName} with key \"${content.key}\"" }
        
        val authentication = authorizationService.getAuthentication()

        if (!content.hasAccess(authentication, role)) 
            throw NotAuthorizedException("User does not have sufficient permission to perform this action. Required role: $role")

        return content
    }

    override suspend fun save(doc: T): T {
        doc.updatedAt = Instant.now()
        return super.save(doc)
    }

    /**
     * Validates if the current user has sufficient permission.
     *
     * @param key The key of the content.
     * @param role The [ContentAccessRole] required to perform the action, e.g. VIEWER to view or EDIT to edit the article.
     *
     * @throws NotAuthorizedException if the user does not have sufficient permissions.
     */
    open suspend fun findAuthorizedByKey(key: String, role: ContentAccessRole): T {
        logger.debug { "Finding ${collectionClazz.simpleName} by key \"$key\" and validating permission: $role" }

        val content = findByKey(key)

        if (role == ContentAccessRole.VIEWER && content.access.visibility == AccessType.PUBLIC) return content

        return requireAuthorization(content, role)
    }
}
