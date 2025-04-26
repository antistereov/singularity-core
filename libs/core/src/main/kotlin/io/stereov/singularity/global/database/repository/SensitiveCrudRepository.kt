package io.stereov.singularity.global.database.repository

import io.stereov.singularity.global.database.model.EncryptedSensitiveDocument
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface SensitiveCrudRepository<T: EncryptedSensitiveDocument<*>> : CoroutineCrudRepository<T, String>
