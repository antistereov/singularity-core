package io.stereov.singularity.secrets.core.component

import io.stereov.singularity.secrets.core.model.Secret

interface KeyManager {

    suspend fun get(key: String): Secret?
    suspend fun put(key: String, value: String, note: String): Secret
}
