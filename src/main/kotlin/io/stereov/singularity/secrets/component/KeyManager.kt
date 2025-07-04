package io.stereov.singularity.secrets.component

import io.stereov.singularity.secrets.model.Secret
import java.util.*

interface KeyManager {

    suspend fun getSecretById(id: UUID): Secret
    suspend fun getSecretByKey(key: String): Secret?

    suspend fun create(key: String, value: String, note: String): Secret
    suspend fun createOrUpdateKey(key: String, value: String, note: String): Secret
    suspend fun update(id: UUID, key: String, value: String, note: String): Secret
}
