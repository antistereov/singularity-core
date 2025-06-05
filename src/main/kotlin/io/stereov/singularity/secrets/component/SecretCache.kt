package io.stereov.singularity.secrets.component

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.secrets.model.CachedSecret
import io.stereov.singularity.secrets.model.Secret
import io.stereov.singularity.secrets.properties.KeyManagerProperties
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * A cache for secrets loaded from a key manager.
 * It can be used to improve performance when working with a key manager.
 * The secrets will expire and be deleted automatically. It is configured in the [KeyManagerProperties].
 */
@Component
class SecretCache(keyManagerProperties: KeyManagerProperties) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    private val expirationDurationSeconds = keyManagerProperties.cacheExpiration
    private val cache = ConcurrentHashMap<UUID, CachedSecret>()

    fun put(secret: Secret) {
        val expirationTime = Instant.now().plusSeconds(expirationDurationSeconds)
        cache[secret.id] = CachedSecret(secret, expirationTime)
    }

    fun get(id: UUID): Secret? {
        val cached = cache[id]
        if (cached != null && cached.expirationTime.isAfter(Instant.now())) {
            put(cached.secret)
            return cached.secret
        }

        cache.remove(id)
        return null
    }

    fun getByKey(key: String): Secret? {
        return cache.entries.firstOrNull { it.value.secret.key == key }?.value?.secret
    }

    fun remove(id: UUID) {
        cache.remove(id)
    }

    fun clear() {
        cache.clear()
    }

    fun cleanupExpiredEntries() {
        val now = Instant.now()
        cache.entries.removeIf { it.value.expirationTime.isBefore(now) }
    }

    @Scheduled(fixedRateString = "\${singularity.secrets.cache-expiration:900000}")
    fun scheduledCleanup() {
        logger.debug { "Starting scheduled cleanup of secret cache" }
        cleanupExpiredEntries()
    }
}
