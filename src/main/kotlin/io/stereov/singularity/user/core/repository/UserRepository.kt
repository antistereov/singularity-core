package io.stereov.singularity.user.core.repository

import io.stereov.singularity.database.core.repository.SensitiveCrudRepository
import io.stereov.singularity.database.hash.model.SearchableHash
import io.stereov.singularity.user.core.model.EncryptedUserDocument
import io.stereov.singularity.user.core.model.Role
import kotlinx.coroutines.flow.Flow
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : SensitiveCrudRepository<EncryptedUserDocument> {

    suspend fun existsByEmail(email: SearchableHash): Boolean

    suspend fun findByEmail(email: SearchableHash): EncryptedUserDocument?

    @Query("{ 'identities.?0.principalId' : ?1 }")
    suspend fun findByIdentity(provider: String, principalId: SearchableHash): EncryptedUserDocument?

    suspend fun findAllByRolesContaining(role: Role): Flow<EncryptedUserDocument>
}
