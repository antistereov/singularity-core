package io.stereov.singularity.auth.group.repository

import io.stereov.singularity.auth.group.model.GroupDocument
import org.bson.types.ObjectId
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface GroupRepository : CoroutineCrudRepository<GroupDocument, ObjectId> {

    suspend fun existsByKey(key: String): Boolean

    suspend fun findByKey(key: String): GroupDocument?

    suspend fun deleteByKey(key: String)
}
