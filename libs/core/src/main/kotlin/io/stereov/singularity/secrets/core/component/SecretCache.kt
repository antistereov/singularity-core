package io.stereov.singularity.secrets.core.component

import com.github.michaelbull.result.Result
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.cache.exception.CacheException
import io.stereov.singularity.cache.service.CacheService
import io.stereov.singularity.secrets.core.model.Secret
import io.stereov.singularity.secrets.core.properties.SecretStoreProperties
import org.springframework.stereotype.Component

/**
 * A cache for secrets loaded from a key manager.
 * It can be used to improve performance when working with a key manager.
 * The secrets will expire and be deleted automatically. It is configured in the [SecretStoreProperties].
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

    suspend fun put(secret: Secret): Result<Secret, CacheException> {
        logger.trace { "Caching secret ${secret.key}" }
        return cacheService.put(getCacheKey(secret.key), secret, expirationDurationSeconds)
    }

    suspend fun get(key: String): Result<Secret, CacheException> {
        logger.trace { "Retrieving secret $key from cache" }
        return cacheService.get<Secret>(getCacheKey(key))
    }
}
