package io.stereov.singularity.user.repository

import io.stereov.singularity.database.repository.SensitiveCrudRepository
import io.stereov.singularity.hash.model.SearchableHash
import io.stereov.singularity.user.model.EncryptedUserDocument
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
interface UserRepository : SensitiveCrudRepository<EncryptedUserDocument> {

    suspend fun existsByEmail(email: SearchableHash): Boolean

    suspend fun findByEmail(email: SearchableHash): EncryptedUserDocument?
}
