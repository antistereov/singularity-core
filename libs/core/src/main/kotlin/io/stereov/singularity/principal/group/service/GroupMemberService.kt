package io.stereov.singularity.principal.group.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.database.encryption.exception.EncryptedDatabaseException
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.principal.core.service.UserService
import io.stereov.singularity.principal.group.exception.GroupMemberException
import org.bson.types.ObjectId
import org.springframework.stereotype.Service

/**
 * Service responsible for managing the membership of users in groups. Provides functionality
 * for adding and removing users from groups while handling necessary operations such as
 * user validation, group existence checks, and cache invalidation.
 */
@Service
class GroupMemberService(
    private val userService: UserService,
    private val groupService: GroupService,
    private val accessTokenCache: AccessTokenCache
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Adds a user to a specified group.
     *
     * @param userId The unique identifier of the user being added to the group.
     * @param groupKey The unique key identifying the group to which the user will be added.
     * @return A [Result] containing the updated [User] object if the operation is successful,
     * or a [GroupMemberException] if an error occurs.
     */
    suspend fun add(
        userId: ObjectId,
        groupKey: String
    ): Result<User, GroupMemberException> = coroutineBinding {
        logger.debug { "Adding user \"$userId\" to group \"$groupKey\"" }

        val groupExists = groupService.existsByKey(groupKey)
            .mapError { ex -> GroupMemberException.Database("Failed to check existence of group with key $groupKey: ${ex.message}", ex) }
            .bind()

        if (groupExists) {
            Err(GroupMemberException.GroupNotFound("Group with key \"$groupKey\" does not exist"))
                .bind()
        }

        val user = userService.findById(userId)
            .mapError { ex -> when(ex) {
                is EncryptedDatabaseException.NotFound -> GroupMemberException.UserNotFound("User with ID $userId does not exist")
                else -> GroupMemberException.Database("Failed to find user with ID $userId: ${ex.message}", ex)
            } }
            .bind()

        user.groups.add(groupKey)

        userService.save(user)
            .mapError { ex -> GroupMemberException.Database("Failed to save updated user to database: ${ex.message}", ex) }
            .onSuccess {
                accessTokenCache.invalidateAllTokens(userId)
                    .onFailure { exception ->
                        logger.error(exception) { "Failed to invalidate all access tokens for user $userId" }
                    }
            }
            .bind()
    }

    /**
     * Removes a user from a specified group.
     *
     * @param userId The unique identifier of the user being removed from the group.
     * @param groupKey The unique key identifying the group from which the user will be removed.
     * @return A [Result] containing the updated [User] object if the operation is successful,
     * or a [GroupMemberException] if an error occurs.
     */
    suspend fun remove(
        userId: ObjectId,
        groupKey: String
    ): Result<User, GroupMemberException> = coroutineBinding {
        logger.debug { "Removing user \"$userId\" from group \"$groupKey\""}

        val groupExists = groupService.existsByKey(groupKey)
            .mapError { ex -> GroupMemberException.Database("Failed to check existence of group with key $groupKey: ${ex.message}", ex) }
            .bind()

        if (groupExists) {
            Err(GroupMemberException.GroupNotFound("Group with key \"$groupKey\" does not exist"))
                .bind()
        }

        val user = userService.findById(userId)
            .mapError { ex -> when(ex) {
                is EncryptedDatabaseException.NotFound -> GroupMemberException.UserNotFound("User with ID $userId does not exist")
                else -> GroupMemberException.Database("Failed to find user with ID $userId: ${ex.message}", ex)
            } }
            .bind()

        user.groups.remove(groupKey)

        userService.save(user)
            .mapError { ex -> GroupMemberException.Database("Failed to save updated user to database: ${ex.message}", ex) }
            .onSuccess {
                accessTokenCache.invalidateAllTokens(userId)
                    .onFailure { exception ->
                        logger.error(exception) { "Failed to invalidate all access tokens for user $userId" }
                    }
            }
            .bind()
    }
}
