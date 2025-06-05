package io.stereov.singularity.database.repository

import io.stereov.singularity.database.model.EncryptedSensitiveDocument
import org.bson.types.ObjectId
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface SensitiveCrudRepository<T: EncryptedSensitiveDocument<*>> : CoroutineCrudRepository<T, ObjectId>
