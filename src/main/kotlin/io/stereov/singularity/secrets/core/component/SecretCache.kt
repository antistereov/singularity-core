package io.stereov.singularity.secrets.core.component

import com.github.michaelbull.result.Result
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.cache.exception.CacheException
import io.stereov.singularity.cache.service.CacheService
import io.stereov.singularity.secrets.core.model.Secret
import io.stereov.singularity.secrets.core.properties.SecretStoreProperties
import org.springframework.stereotype.Component

/**
 * A caching service for handling sensitive [Secret] objects.
 * This class serves as a layer over the CacheService, providing functionality to cache and retrieve
 * [Secret] objects using specific keys. The caching is configured with an expiration time derived
 * from the [SecretStoreProperties] configuration.
 *
 * This class uses a prefixed key scheme to ensure secrets are namespace-isolated in the cache.
 * Logging is provided at the trace level for cache actions.
 *
 * @param secretStoreProperties Configuration for the secret storage including cache expiration settings.
 * @param cacheService The underlying service responsible for interacting with the cache infrastructure (e.g., Redis).
 */
@Component
class SecretCache(
    secretStoreProperties: SecretStoreProperties,
    private val cacheService: CacheService
) {

    private val logger = KotlinLogging.logger {}
    private val prefix = "secrets"

    private fun getCacheKey(key: String) = "$prefix:$key"
    private val expirationDurationSeconds = secretStoreProperties.cacheExpiration

    /**
     * Caches the given secret in the underlying cache service with an expiration duration.
     *
     * This method uses a prefixed key scheme to store the secret in the cache,
     * ensuring namespace isolation. The expiration duration is configured via
     * the secret store properties.
     *
     * @param secret The secret to cache. It contains the key, value, and metadata information.
     * @return A [Result] containing the stored secret if the operation is successful,
     *   or a [CacheException] if an error occurs during the caching process.
     */
    suspend fun put(secret: Secret): Result<Secret, CacheException> {
        logger.trace { "Caching secret ${secret.key}" }
        return cacheService.put(getCacheKey(secret.key), secret, expirationDurationSeconds)
    }

    /**
     * Retrieves a cached secret associated with the given key.
     *
     * This function uses the underlying cache service to fetch a `HashSecret` object
     * stored under a key derived using a prefixed naming convention. If the key
     * does not exist or another issue arises, a `CacheException` will be returned.
     *
     * @param key The key used to retrieve the associated secret from the cache.
     * @return A [Result] containing the retrieved [Secret] if successful, or a [CacheException] if
     * an error occurs (e.g., key not found or deserialization failure).
     */
    suspend fun get(key: String): Result<Secret, CacheException> {
        logger.trace { "Retrieving secret $key from cache" }
        return cacheService.get<Secret>(getCacheKey(key))
    }
}
