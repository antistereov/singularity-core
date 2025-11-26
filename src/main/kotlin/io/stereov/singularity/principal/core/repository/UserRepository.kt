package io.stereov.singularity.principal.core.repository

import io.stereov.singularity.database.encryption.repository.SensitiveCrudRepository
import io.stereov.singularity.database.hash.model.SearchableHash
import io.stereov.singularity.principal.core.model.encrypted.EncryptedUser
import io.stereov.singularity.principal.core.model.Role
import kotlinx.coroutines.flow.Flow
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : SensitiveCrudRepository<EncryptedUser> {

    suspend fun existsByEmail(email: SearchableHash): Boolean

    suspend fun findByEmail(email: SearchableHash): EncryptedUser?

    @Query("{ 'identities.providers.?0.principalId' : ?1 }")
    suspend fun findByIdentity(provider: String, principalId: SearchableHash): EncryptedUser?

    suspend fun findAllByRolesContaining(role: Role): Flow<EncryptedUser>

    suspend fun findAllByGroupsContaining(group: String): Flow<EncryptedUser>
}
