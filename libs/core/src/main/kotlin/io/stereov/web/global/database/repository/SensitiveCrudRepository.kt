package io.stereov.web.global.database.repository

import io.stereov.web.global.database.model.EncryptedSensitiveDocument
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface SensitiveCrudRepository<T: EncryptedSensitiveDocument<*>> : CoroutineCrudRepository<T, String>
