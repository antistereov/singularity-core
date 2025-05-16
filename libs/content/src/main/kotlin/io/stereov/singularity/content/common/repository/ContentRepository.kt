package io.stereov.singularity.content.common.repository

import io.stereov.singularity.content.common.model.ContentDocument
import org.bson.types.ObjectId
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface ContentRepository<T: ContentDocument<T>> : CoroutineCrudRepository<T, ObjectId> {

    suspend fun findByKey(key: String): T?

    suspend fun existsByKey(key: String): Boolean
}
