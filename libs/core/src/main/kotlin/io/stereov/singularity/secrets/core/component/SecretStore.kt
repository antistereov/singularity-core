package io.stereov.singularity.secrets.core.component

import com.github.michaelbull.result.*
import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.secrets.core.exception.SecretStoreException
import io.stereov.singularity.secrets.core.model.Secret

/**
 * Represents an interface for managing secrets in a secure and efficient way.
 * Provides methods for retrieving and storing secrets with caching capabilities.
 */
interface SecretStore {

    val secretCache: SecretCache
    val logger: KLogger

    /**
     * Retrieves a secret associated with the given key.
     * The method first attempts to fetch the secret from a local cache.
     * If the secret is not found in the cache, it fetches the secret from the underlying store
     * and caches the result for future access.
     *
     * @param key The key of the secret to be retrieved.
     * @return A [Result] containing the retrieved [Secret] if the operation succeeds,
     * or a [SecretStoreException] if it fails.
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
     * Stores a secret with the specified key, value, and an optional note in the secret store.
     * The method first attempts to persist the secret using the underlying store implementation.
     * Upon success, it caches the result locally for faster subsequent retrievals.
     *
     * @param key The unique key associated with the secret.
     * @param value The value of the secret to be stored.
     * @param note An optional descriptive note related to the secret. Default is an empty string.
     * @return A [Result] containing the stored [Secret] if the operation succeeds,
     * or a [SecretStoreException] if there is a failure.
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
