package io.stereov.singularity.secrets.core.component

import com.github.michaelbull.result.*
import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.secrets.core.exception.SecretStoreException
import io.stereov.singularity.secrets.core.model.Secret

/**
 * Retrieve and store secrets from the configured store while caching the secrets.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
interface SecretStore {

    val secretCache: SecretCache
    val logger: KLogger

    /**
     * Get a secret based on the [key].
     * It will check the cache first and then try to get the secret from the configured store.
     *
     * @param key The key of the secret.
     *
     * @return The [Secret].
     */
    suspend fun get(key: String): Result<Secret, SecretStoreException> {
        return secretCache.get(key).flatMapEither(
            { secret -> Ok(secret) },
            { ex ->
                doGet(key).onSuccess { secret ->
                    secretCache.put(secret).recover { ex ->
                        logger.warn(ex) { "Failed to cache secret $key: ${ex.message}" }
                    }
            } }
        )
    }

    /**
     * Put a secret in the configured store.
     * It will automatically cache the secret.
     *
     * @param key The key of the secret.
     * @param value The value of the secret.
     * @param note An optional note.
     *
     * @return The saved [Secret].
     */
    suspend fun put(key: String, value: String, note: String = ""): Result<Secret, SecretStoreException> {
        return doPut(key, value, note).onSuccess { secret ->
            secretCache.put(secret).recover { ex ->
                logger.warn(ex) { "Failed to cache secret $key: ${ex.message}"}
            }
        }
    }

    suspend fun doGet(key: String): Result<Secret, SecretStoreException>
    suspend fun doPut(key: String, value: String, note: String = ""): Result<Secret, SecretStoreException>
}
