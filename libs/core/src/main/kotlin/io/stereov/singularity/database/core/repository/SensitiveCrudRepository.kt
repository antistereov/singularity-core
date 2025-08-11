package io.stereov.singularity.database.core.repository

import io.stereov.singularity.database.core.model.EncryptedSensitiveDocument
import org.bson.types.ObjectId
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface SensitiveCrudRepository<T: EncryptedSensitiveDocument<*>> : CoroutineCrudRepository<T, ObjectId>
