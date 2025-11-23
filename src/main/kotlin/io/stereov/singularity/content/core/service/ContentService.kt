package io.stereov.singularity.content.core.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.coroutines.runSuspendCatching
import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.group.model.KnownGroups
import io.stereov.singularity.content.core.component.AccessCriteria
import io.stereov.singularity.content.core.exception.ContentException
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

/**
 * Abstract class representing a content service for handling content-related CRUD operations and authorization.
 *
 * @param T The type of content document that this service manages. It must extend from [ContentDocument].
 */
abstract class ContentService<T: ContentDocument<T>> : CrudService<T>  {

    abstract override val repository: ContentRepository<T>
    abstract val authorizationService: AuthorizationService
    abstract val translateService: TranslateService
    abstract val accessCriteria: AccessCriteria
    abstract val contentProperties: ContentProperties
    abstract val contentType: String

    /**
     * Constructs a URL from the provided key by replacing placeholders in the content URI template.
     * This method validates that the resulting URI complies with RFC 2396 and converts it to a URL.
     *
     * @param key The unique identifier used to replace the placeholder {contentKey} in the content URI.
     * @return A [Result] containing the constructed [URL] if successful, or an [IllegalArgumentException] if the URI format is invalid.
     */
    fun getUri(key: String): Result<URL, IllegalArgumentException> = runCatching {
        URI.create(
            contentProperties.contentUri
                .replace("{contentType}", contentType)
                .replace("{contentKey}", key)
        ).toURL()
    }.mapError { ex -> IllegalArgumentException("The given string violates RFC 2396", ex) }

    /**
     * Finds an entity by its unique key in the database.
     * This method returns a result that contains the entity if found or an appropriate exception
     * in case of an error or if the entity is not found.
     *
     * @param key The unique identifier of the entity to search for.
     * @return A [Result] containing the entity of type [T] if found, or an instance of [DatabaseException]
     * in case of a database error or if the entity is not found.
     */
    suspend fun findByKey(key: String): Result<T, DatabaseException>  {
        logger.debug { "Finding ${collectionClazz.simpleName} by key \"$key\"" }

        return runSuspendCatching { repository.findByKey(key) }
            .mapError { ex -> DatabaseException.Database("Failed to find ${collectionClazz.simpleName}: ${ex.message}", ex) }
            .andThen { it.toResultOr { DatabaseException.NotFound("No ${collectionClazz.simpleName} with key $key found") } }
    }

    /**
     * Deletes an entity associated with the specified key from the database.
     *
     * This method attempts to remove the entity identified by the provided key
     * and handles any database-related errors that might occur during the operation.
     *
     * @param key The unique identifier of the entity to be deleted.
     * @return A [Result] containing [Unit] if the entity was successfully deleted,
     * or a [DatabaseException.Database] if an error occurred during the operation.
     */
    open suspend fun deleteByKey(key: String): Result<Unit, DatabaseException.Database> {
        logger.debug { "Deleting ${collectionClazz.simpleName} with key \"$key\"" }

        return runSuspendCatching { repository.deleteByKey(key) }
            .mapError { ex -> DatabaseException.Database("Failed to delete ${collectionClazz.simpleName} with key $key: ${ex.message}", ex) }
    }

    /**
     * Checks if an entity with the specified key exists in the database.
     *
     * This method performs a database existence check for an entity identified by the given key.
     * If successful, it returns a boolean wrapped in a `Result`, indicating the existence of the entity.
     * In case of a database-related error, an instance of `DatabaseException.Database` is returned.
     *
     * @param key The unique identifier of the entity to check for its existence in the database.
     * @return A [Result] containing `true` if the entity exists, `false` otherwise, or a [DatabaseException.Database]
     * in case of an error during the operation.
     */
    open suspend fun existsByKey(key: String): Result<Boolean, DatabaseException.Database> {
        logger.debug { "Checking if ${collectionClazz.simpleName} with key \"$key\" exists" }

        return runSuspendCatching { repository.existsByKey(key) }
            .mapError { ex -> DatabaseException.Database("Failed to check existence of ${collectionClazz.simpleName} by key $key: ${ex.message}", ex) }
    }

    /**
     * Validates that the authenticated user belongs to the Contributor group.
     * If the user is not a member of the required group, an error is returned.
     *
     * @param authentication The authenticated user whose group membership is being validated.
     * @return A [Result] containing the authenticated user if they belong to the Contributor group,
     *   or an [AuthenticationException.GroupMembershipRequired] if the membership check fails.
     */
    fun requireContributorGroupMembership(
        authentication: AuthenticationOutcome.Authenticated
    ): Result<AuthenticationOutcome.Authenticated, AuthenticationException.GroupMembershipRequired> {
        logger.debug { "Validate that user belongs to Editor group" }

        return authentication.requireGroupMembership(KnownGroups.CONTRIBUTOR)
    }

    /**
     * Validates if the authenticated user has the required access role to perform an action
     * on the given content. If the user is authorized, the content is returned; otherwise,
     * an error is returned.
     *
     * @param authenticationOutcome The authentication information of the user attempting to perform the action.
     * @param content The content on which the action is to be performed.
     * @param role The role required to perform the action, such as VIEWER, EDITOR, or ADMIN.
     * @return A [Result] containing the content of type [T] if the user is authorized, or a [ContentException.NotAuthorized]
     * if the user does not have sufficient permissions.
     */
    fun requireAuthorization(
        authenticationOutcome: AuthenticationOutcome,
        content: T, role: ContentAccessRole
    ): Result<T, ContentException.NotAuthorized> {
        logger.debug { "Validating that user has role \"$role\" in ${collectionClazz.simpleName} with key \"${content.key}\"" }

        return if (content.hasAccess(authenticationOutcome, role)) {
            Ok(content)
        } else {
            Err(ContentException.NotAuthorized("User does not have sufficient permission to perform this action on $contentType with key ${content.key}: Role $role required"))
        }
    }

    /**
     * Saves the given document after updating its timestamp to the current time.
     *
     * @param doc The document to be saved. The `updatedAt` property of this document is updated before saving.
     * @return A result containing the saved document if successful, or a [DatabaseException.Database] in case of a failure.
     */
    override suspend fun save(doc: T): Result<T, DatabaseException.Database> {
        doc.updatedAt = Instant.now()
        return super.save(doc)
    }

    /**
     * Finds content by its unique key and validates the user's permission to access it.
     *
     * This method retrieves the content associated with the given key, validates the provided
     * authentication token, and ensures the user has the required access role for the content.
     * If the user does not meet the authentication or authorization requirements, an error is returned.
     *
     * @param key The unique identifier for the content to retrieve.
     * @param authenticationOutcome The authentication token of the user attempting to access the content.
     *   Can be null if no user is authenticated.
     * @param role The access role required to retrieve the content (e.g., VIEWER, EDITOR, MAINTAINER).
     * @return A [Result] containing the content of type [T] if the user is authorized,
     *   or an instance of [ContentException] if an error or unauthorized access occurs.
     */
    open suspend fun findAuthorizedByKey(
        key: String,
        authenticationOutcome: AuthenticationOutcome,
        role: ContentAccessRole
    ): Result<T, ContentException> = coroutineBinding {
        logger.debug { "Finding ${collectionClazz.simpleName} by key \"$key\" and validating permission: $role" }

        val content = findByKey(key)
            .mapError { ex -> ContentException.fromDatabaseException(ex) }
            .bind()

        requireAuthorization(authenticationOutcome, content, role).bind()
    }
}
