package io.stereov.web.user.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.user.exception.model.UserDoesNotExistException
import io.stereov.web.user.model.UserDocument
import io.stereov.web.user.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

/**
 * # Service for managing user accounts.
 *
 * This service provides methods to find, save, and delete user accounts.
 * It interacts with the [UserRepository] to perform database operations.
 *
 * @author antistereov
 */
@Service
class UserService(
    private val userRepository: UserRepository,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Finds a user by their ID.
     *
     * @param userId The ID of the user to find.
     *
     * @return The [UserDocument] of the found user.
     *
     * @throws UserDoesNotExistException If no user is found with the given ID.
     */
    suspend fun findById(userId: String): UserDocument {
        logger.debug { "Finding user by ID: $userId" }

        return userRepository.findById(userId)
            ?: throw UserDoesNotExistException("No user account found with id $userId")
    }

    /**
     * Finds a user by their ID, returning null if not found.
     *
     * @param userId The ID of the user to find.
     *
     * @return The [UserDocument] of the found user, or null if not found.
     */
    suspend fun findByIdOrNull(userId: String): UserDocument? {
        logger.debug { "Finding user by ID: $userId" }

        return userRepository.findById(userId)
    }

    /**
     * Finds a user by their email address.
     *
     * @param email The email address of the user to find.
     *
     * @return The [UserDocument] of the found user.
     *
     * @throws UserDoesNotExistException If no user is found with the given email.
     */
    suspend fun findByEmail(email: String): UserDocument {
        logger.debug { "Fetching user with email $email" }

        return userRepository.findByEmail(email)
            ?: throw UserDoesNotExistException("No user account found with email $email")
    }

    /**
     * Finds a user by their email address, returning null if not found.
     *
     * @param email The email address of the user to find.
     *
     * @return The [UserDocument] of the found user, or null if not found.
     */
    suspend fun findByEmailOrNull(email: String): UserDocument? {
        logger.debug { "Fetching user with email $email" }

        return userRepository.findByEmail(email)
    }

    /**
     * Checks if a user exists by their email address.
     *
     * @param email The email address to check.
     *
     * @return True if a user with the given email exists, false otherwise.
     */
    suspend fun existsByEmail(email: String): Boolean {
        logger.debug { "Checking if email $email already exists" }

        return userRepository.existsByEmail(email)
    }

    /**
     * Saves a user account.
     *
     * @param user The [UserDocument] to save.
     *
     * @return The saved [UserDocument].
     */
    suspend fun save(user: UserDocument): UserDocument {
        logger.debug { "Saving user: ${user.id}" }

        user.updateLastActive()

        val savedUser = userRepository.save(user)

        logger.debug { "Successfully saved user" }

        return savedUser
    }

    /**
     * Deletes a user account by their ID.
     */
    suspend fun deleteById(userId: String) {
        logger.debug { "Deleting user $userId" }

        userRepository.deleteById(userId)
    }

    /**
     * Deletes all user accounts.
     *
     * @throws Exception If an error occurs during deletion.
     */
    suspend fun deleteAll() {
        logger.debug { "Deleting all user accounts" }

        return userRepository.deleteAll()
    }

    /**
     * Finds all user accounts.
     *
     * @return A [Flow] of [UserDocument] representing all user accounts.
     */
    suspend fun findAll(): Flow<UserDocument> {
        logger.debug { "Finding all user accounts" }

        return userRepository.findAll()
    }
}
