package io.stereov.singularity.secrets.core.component

import io.stereov.singularity.secrets.core.exception.model.SecretKeyNotFoundException
import io.stereov.singularity.secrets.core.model.Secret

interface SecretStore {

    suspend fun getOrNull(key: String): Secret?
    suspend fun get(key: String): Secret {
        return getOrNull(key) ?: throw SecretKeyNotFoundException(key)
    }
    suspend fun put(key: String, value: String, note: String = ""): Secret
}
