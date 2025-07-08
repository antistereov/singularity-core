package io.stereov.singularity.content.common.content.repository

import io.stereov.singularity.content.common.content.model.ContentDocument
import org.bson.types.ObjectId
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface ContentRepository<T: ContentDocument<T>> : CoroutineCrudRepository<T, ObjectId> {

    suspend fun findByKey(key: String): T?
    suspend fun deleteByKey(key: String)
    suspend fun existsByKey(key: String): Boolean
}
