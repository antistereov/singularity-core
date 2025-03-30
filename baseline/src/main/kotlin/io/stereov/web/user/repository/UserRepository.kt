package io.stereov.web.user.repository

import io.stereov.web.user.model.UserDocument
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

/**
 * # Repository for managing user accounts.
 *
 * This repository provides methods to perform CRUD operations on user accounts.
 * It extends the [CoroutineCrudRepository] interface to support
 * coroutine-based operations.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Repository
interface UserRepository : CoroutineCrudRepository<UserDocument, String> {

    /**
     * Finds a user by their ID.
     *
     * @param userId The ID of the user to find.
     *
     * @return The [UserDocument] of the found user, or null if not found.
     */
    suspend fun existsByEmail(email: String): Boolean

    /**
     * Finds a user by their email address.
     *
     * @param email The email address of the user to find.
     *
     * @return The [UserDocument] of the found user, or null if not found.
     */
    suspend fun findByEmail(email: String): UserDocument?
}
