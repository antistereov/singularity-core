package io.stereov.singularity.core.global.database.repository

import io.stereov.singularity.core.global.database.model.EncryptedSensitiveDocument
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface SensitiveCrudRepository<T: EncryptedSensitiveDocument<*>> : CoroutineCrudRepository<T, String>
