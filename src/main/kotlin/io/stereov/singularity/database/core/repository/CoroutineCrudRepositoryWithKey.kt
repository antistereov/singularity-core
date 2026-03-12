package io.stereov.singularity.database.core.repository

import io.stereov.singularity.database.core.model.DocumentKey
import io.stereov.singularity.database.core.model.WithKey
import org.bson.types.ObjectId
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface CoroutineCrudRepositoryWithKey<D : WithKey> : CoroutineCrudRepository<D, ObjectId> {

    suspend fun existsByKey(key: DocumentKey): Boolean
    suspend fun findByKey(key: DocumentKey): D?
    suspend fun deleteByKey(key: DocumentKey)
}