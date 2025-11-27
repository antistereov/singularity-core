package io.stereov.singularity.principal.group.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.coroutines.runSuspendCatching
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.stereov.singularity.database.core.exception.DatabaseException
import io.stereov.singularity.database.encryption.exception.EncryptionException
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.principal.core.service.UserService
import io.stereov.singularity.principal.group.dto.request.CreateGroupRequest
import io.stereov.singularity.principal.group.dto.request.UpdateGroupRequest
import io.stereov.singularity.principal.group.exception.CreateGroupException
import io.stereov.singularity.principal.group.exception.DeleteGroupByKeyException
import io.stereov.singularity.principal.group.exception.FindGroupByKeyException
import io.stereov.singularity.principal.group.exception.UpdateGroupException
import io.stereov.singularity.principal.group.mapper.GroupMapper
import io.stereov.singularity.principal.group.model.Group
import io.stereov.singularity.principal.group.model.GroupTranslation
import io.stereov.singularity.principal.group.repository.GroupRepository
import io.stereov.singularity.translate.service.TranslatableCrudService
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.reactive.collect
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Service

/**
 * A service class for managing [Group]-related operations within the application.
 *
 * This class provides functionalities such as creating, retrieving, updating,
 * and deleting groups, along with validation and data consistency checks.
 */
@Service
class GroupService(
    override val repository: GroupRepository,
    override val appProperties: AppProperties,
    override val reactiveMongoTemplate: ReactiveMongoTemplate,
    private val groupMapper: GroupMapper,
    private val userService: UserService
) : TranslatableCrudService<GroupTranslation, Group> {

    override val logger = logger {}
    override val collectionClazz = Group::class.java
    override val contentClass = GroupTranslation::class.java

    @PostConstruct
    fun initializeGroups() = runBlocking {
        logger.info { "Creating initial groups" }

        appProperties.groups.forEach { groupRequest ->
            runBlocking {
                create(groupRequest)
                    .onSuccess { logger.info { "Created group with key \"${groupRequest.key}\""} }
                    .onFailure { ex -> logger.error(ex) { "Failed to create group with key \"${groupRequest.key}\""}}
            }
        }
    }

    /**
     * Creates a new group based on the provided request.
     *
     * @param req The request object containing details for creating the group, including the key and translations.
     * @return A [Result] representing either the created group on success or a [CreateGroupException] on failure.
     */
    suspend fun create(req: CreateGroupRequest): Result<Group, CreateGroupException> {
        logger.debug { "Creating group with key \"${req.key}\"" }

        if (req.translations.isEmpty()) {
            return Err(CreateGroupException.InvalidGroupTranslation("Failed to create group: at least one translation is needed"))
        }

        if (!req.translations.containsKey(appProperties.locale)) {
            return Err(CreateGroupException.InvalidGroupTranslation("Failed to create group: default locale ${appProperties.locale} is not contained in translations"))
        }

        return existsByKey(req.key)
            .mapError { ex -> CreateGroupException.Database("Failed to check existence of group with key ${req.key}: ${ex.message}", ex) }
            .andThen { exists ->
                if (exists) {
                    Err(CreateGroupException.KeyExists("Failed to create group: group with key ${req.key} already exists"))
                } else {
                    save(groupMapper.createGroup(req))
                        .mapError { ex -> CreateGroupException.Database("Failed to save group: ${ex.message}", ex) }
            }
        }
    }

    /**
     * Suspends and retrieves a group document by the provided key.
     *
     * @param key The unique key identifying the group to be retrieved.
     * @return A [Result] containing the [Group] if found, or a [FindGroupByKeyException]
     *  detailing the failure reason if the operation is unsuccessful.
     */
    suspend fun findByKey(key: String): Result<Group, FindGroupByKeyException> {
        logger.debug { "Finding group by key \"$key\"" }

        return runSuspendCatching { repository.findByKey(key) }
            .mapError { ex -> FindGroupByKeyException.Database("Failed to check existence of group with key $key: ${ex.message}", ex) }
            .andThen { it.toResultOr { FindGroupByKeyException.NotFound("No group with key \"$key\" found") }}
    }

    /**
     * Checks if a group with the specified key exists in the database.
     *
     * @param key The unique key to check for existence in the database.
     * @return A [Result] containing true if the group exists, false otherwise.
     *  Returns a failure with [DatabaseException.Database] in case of an error.
     */
    suspend fun existsByKey(key: String): Result<Boolean, DatabaseException.Database> {
        logger.debug { "Checking if group with key \"$key\" exists" }

        return runSuspendCatching { repository.existsByKey(key) }
            .mapError { ex -> DatabaseException.Database("Failed to check existence of group with key $key: ${ex.message}", ex) }
    }

    /**
     * Updates a group document identified by the provided key with the data specified in the request.
     *
     * @param key The unique key identifying the group to be updated.
     * @param req The update request containing the new translations, as well as translations to delete.
     * @return A [Result] containing the updated [Group] if the operation is successful,
     * or an [UpdateGroupException] if an error occurs.
     */
    suspend fun update(
        key: String,
        req: UpdateGroupRequest
    ): Result<Group, UpdateGroupException> = coroutineBinding {
        logger.debug { "Updating group with key \"$key\"" }

        val group = findByKey(key)
            .mapError { ex -> when (ex) {
                is FindGroupByKeyException.NotFound -> UpdateGroupException.NotFound("No group with key \"$key\" found")
                else -> UpdateGroupException.Database("Failed to find group with key $key: ${ex.message}", ex) }
            }
            .bind()

        req.translationsToDelete.forEach { locale -> group.translations.remove(locale) }
        group.translations.putAll(req.translations)

        if (!group.translations.containsKey(appProperties.locale)) {
            Err(UpdateGroupException.InvalidGroupTranslation("Failed to update group: default locale ${appProperties.locale} is not contained in translations"))
                .bind()
        }

        save(group)
            .mapError { ex -> UpdateGroupException.Database("Failed to save updated group: ${ex.message}", ex) }
            .bind()
    }

    /**
     * Deletes a group identified by the given key. 
     * This includes updating all associated users
     * to remove the group from their group list and deleting the group from the repository.
     *
     * @param key The unique identifier of the group to be deleted.
     * @return A [Result] containing [Unit] if the operation is successful, or a [DeleteGroupByKeyException]
     * if an error occurs during the deletion process, such as issues with database operations
     * or failure to update users.
     */
    suspend fun deleteByKey(key: String): Result<Unit, DeleteGroupByKeyException> = coroutineBinding {
        logger.debug { "Deleting group with key \"$key\"" }

        val exists = existsByKey(key)
            .mapError { ex -> DeleteGroupByKeyException.Database("Failed to check existence of group with key $key: ${ex.message}", ex) }
            .bind()

        if (!exists) {
            Err(DeleteGroupByKeyException.NotFound("No group with key \"$key\" found"))
                .bind()
        }

        userService.findAllByGroupContaining(key)
            .asFlux()
            .map { user ->
                user.onSuccess { it.groups.remove(key) }
            }
            .buffer(1000)
            .collect { results ->
                val errors = mutableListOf<EncryptionException>()
                val successes = mutableListOf<User>()
                
                results.forEach { result ->
                    result.onSuccess { successes.add(it) }
                        .onFailure { ex -> errors.add(ex) }
                }

                userService.saveAll(successes)
                    .mapError { ex -> DeleteGroupByKeyException.MemberUpdate("Failed to save updated users: ${ex.message}", ex) }
                    .bind()
                
                if (errors.isNotEmpty()) {
                    Err(DeleteGroupByKeyException.MemberUpdate("Failed to generate ${errors.size} affected users: ${errors.joinToString("; ") { it.message }}"))
                        .bind()
                }
            }

        runSuspendCatching { repository.deleteByKey(key) }
        .mapError { ex -> DeleteGroupByKeyException.Database("Failed to delete group with key $key: ${ex.message}", ex) }
            .bind()
    }
}
