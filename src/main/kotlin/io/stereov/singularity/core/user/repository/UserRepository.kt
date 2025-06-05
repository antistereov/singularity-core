package io.stereov.singularity.core.user.repository

import io.stereov.singularity.core.global.database.repository.SensitiveCrudRepository
import io.stereov.singularity.core.hash.model.SearchableHash
import io.stereov.singularity.core.user.model.EncryptedUserDocument
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
