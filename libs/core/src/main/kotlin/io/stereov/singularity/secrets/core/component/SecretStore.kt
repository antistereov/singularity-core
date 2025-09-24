package io.stereov.singularity.secrets.core.component

import io.stereov.singularity.secrets.core.exception.model.SecretKeyNotFoundException
import io.stereov.singularity.secrets.core.model.Secret

interface SecretStore {

    val secretCache: SecretCache

    suspend fun getOrNull(key: String): Secret? {
        val cachedSecret = secretCache.getOrNull(key)

        if (cachedSecret != null) return cachedSecret

        val storedSecret = doGetOrNull(key)

        if (storedSecret != null) secretCache.put(storedSecret)

        return storedSecret
    }
    suspend fun get(key: String): Secret {
        return getOrNull(key) ?: throw SecretKeyNotFoundException(key)
    }
    suspend fun put(key: String, value: String, note: String = ""): Secret {
        val storedSecret = doPut(key, value, note)

        secretCache.put(storedSecret)

        return storedSecret
    }

    suspend fun doGetOrNull(key: String): Secret?
    suspend fun doPut(key: String, value: String, note: String = ""): Secret
}
