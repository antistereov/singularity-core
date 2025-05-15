package io.stereov.singularity.core.group.repository

import io.stereov.singularity.core.group.model.GroupDocument
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface GroupRepository : CoroutineCrudRepository<GroupDocument, String> {

    suspend fun findByKey(key: String): GroupDocument?

    suspend fun deleteByKey(key: String)
}
