package io.stereov.singularity.principal.group.repository

import io.stereov.singularity.principal.group.model.Group
import org.bson.types.ObjectId
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface GroupRepository : CoroutineCrudRepository<Group, ObjectId> {

    suspend fun existsByKey(key: String): Boolean
    suspend fun findByKey(key: String): Group?
    suspend fun deleteByKey(key: String)
}
