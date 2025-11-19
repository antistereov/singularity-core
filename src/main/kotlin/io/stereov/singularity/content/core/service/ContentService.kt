package io.stereov.singularity.content.core.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.coroutines.runSuspendCatching
import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.auth.core.exception.model.GroupMembershipRequiredException
import io.stereov.singularity.auth.core.exception.model.NotAuthorizedException
import io.stereov.singularity.auth.core.model.token.AccessType
import io.stereov.singularity.auth.core.model.token.AuthenticationToken
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.group.model.KnownGroups
import io.stereov.singularity.content.core.component.AccessCriteria
import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.content.core.model.ContentDocument
import io.stereov.singularity.content.core.properties.ContentProperties
import io.stereov.singularity.content.core.repository.ContentRepository
import io.stereov.singularity.database.core.exception.DatabaseException
import io.stereov.singularity.database.core.service.CrudService
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

    fun getUri(key: String): Result<URL, IllegalArgumentException> = runCatching {
        URI.create(
            contentProperties.contentUri
                .replace("{contentType}", contentType)
                .replace("{contentKey}", key)
        ).toURL()
    }.mapError { ex -> IllegalArgumentException("The given string violates RFC 2396", ex) }

    suspend fun findByKey(key: String): Result<T, DatabaseException>  {
        logger.debug { "Finding ${collectionClazz.simpleName} by key \"$key\"" }

        return runSuspendCatching { repository.findByKey(key) }
            .mapError { ex -> DatabaseException.Database("Failed to find ${collectionClazz.simpleName}: ${ex.message}", ex) }
            .andThen { it.toResultOr { DatabaseException.NotFound("No ${collectionClazz.simpleName} with key $key found") } }
    }

    open suspend fun deleteByKey(key: String): Result<Unit, DatabaseException.Database> {
        logger.debug { "Deleting ${collectionClazz.simpleName} with key \"$key\"" }

        return runSuspendCatching { repository.deleteByKey(key) }
            .mapError { ex -> DatabaseException.Database("Failed to delete ${collectionClazz.simpleName} with key $key: ${ex.message}", ex) }
    }

    open suspend fun existsByKey(key: String): Result<Boolean, DatabaseException.Database> {
        logger.debug { "Checking if ${collectionClazz.simpleName} with key \"$key\" exists" }

        return runSuspendCatching { repository.existsByKey(key) }
            .mapError { ex -> DatabaseException.Database("Failed to check existence of ${collectionClazz.simpleName} by key $key: ${ex.message}", ex) }
    }

    /**
     * Validate that the current user belongs to the Editor group.
     *
     * @throws GroupMembershipRequiredException if the user does not have sufficient permissions.
     */
    fun requireContributorGroupMembership(authentication: AuthenticationToken): Result<AuthenticationToken, AuthenticationException.GroupMembershipRequired> {
        logger.debug { "Validate that user belongs to Editor group" }

        return authorizationService.requireGroupMembership(authentication, KnownGroups.CONTRIBUTOR)
    }

    fun requireAuthorization(content: T, authentication: AuthenticationToken, role: ContentAccessRole): T {
        logger.debug { "Validating that user has role \"$role\" in ${collectionClazz.simpleName} with key \"${content.key}\"" }

        if (!content.hasAccess(authentication, role)) 
            throw NotAuthorizedException("User does not have sufficient permission to perform this action. Required role: $role")

        return content
    }

    override suspend fun save(doc: T): Result<T, DatabaseException.Database> {
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
    open suspend fun findAuthorizedByKey(key: String, authentication: AuthenticationToken, role: ContentAccessRole): Result<T, DatabaseException> = coroutineBinding {
        logger.debug { "Finding ${collectionClazz.simpleName} by key \"$key\" and validating permission: $role" }

        val content = findByKey(key).bind()

        if (role == ContentAccessRole.VIEWER && content.access.visibility == AccessType.PUBLIC) {
            content
        } else {
            requireAuthorization(content, authentication, role)
        }
    }
}
