package io.stereov.singularity.cache.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.stereov.singularity.cache.exception.model.RedisKeyNotFoundException
import kotlinx.coroutines.reactive.collect
import kotlinx.coroutines.reactor.asFlux
import org.springframework.stereotype.Service

/**
 * # Service for managing Redis cache operations.
 *
 * This service provides methods to save, retrieve, and delete data in Redis.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Service
@OptIn(ExperimentalLettuceCoroutinesApi::class)
class CacheService(
    val redisCommands: RedisCoroutinesCommands<String, ByteArray>,
    val objectMapper: ObjectMapper
) {
    val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Saves a value to Redis with the given key.
     * If a value on this key already existed, it will be replaced.
     *
     * @param key The key to save the value under.
     * @param value The value to save.
     * @param expiresIn The number of seconds until the key expires. If null, it will never expire.
     */
    suspend fun <T: Any> put(key: String, value: T, expiresIn: Long? = null): T {
        logger.debug { "Saving key $key to Redis" }

        val string = objectMapper.writeValueAsString(value)
        if (expiresIn != null) {
            redisCommands.setex(key, expiresIn, string.toByteArray())
        } else {
            redisCommands.set(key, string.toByteArray())
        }

        return value
    }

    /**
     * Checks if a value saved on the given key exists.
     *
     * @param key The key.
     *
     * @return True if a value on this key exists, of false if not.
     */
    suspend fun exists(key: String): Boolean {
        logger.debug { "Checking if value for key $key exists" }

        return redisCommands.exists(key)?.let { it > 0 } ?: false
    }

    /**
     * Retrieves a value from Redis by its key.
     *
     * @param key The key to retrieve the value for.
     * @param T The type of the value.
     *
     * @return The value associated with the key.
     *
     * @throws RedisKeyNotFoundException If the key is not found in Redis.
     */
    final suspend inline fun <reified T: Any> get(key: String): T {
        logger.debug { "Getting value for key: $key" }

        return getOrNull(key)
            ?: throw RedisKeyNotFoundException(key)
    }

    /**
     * Retrieves a value from Redis by its key.
     *
     * @param key The key to retrieve the value for.
     * @param T The type of the data.
     *
     * @return The value associated with the key, or null if not found.
     */
    final suspend inline fun <reified T: Any>getOrNull(key: String): T? {
        logger.debug { "Getting value for key: $key" }

        return redisCommands.get(key)
            ?.let { objectMapper.readValue<T>(it) }
    }

    /**
     * Deletes one or more keys.
     *
     * This method removes the specified key and its associated value from Redis.
     *
     * @param keys The keys to delete.
     *
     * @return The number of deleted keys.
     */
    suspend fun delete(vararg keys: String): Long? {
        logger.debug { "Deleting data for keys: $keys" }

        return redisCommands.unlink(*keys)
    }

    /**
     * Deletes all data from Redis.
     *
     * This method clears all keys and values from the Redis database.
     *
     * @param pattern Delete only keys matching the pattern.
     */
    suspend fun deleteAll(pattern: String? = null) {
        if (pattern == null) {
            redisCommands.flushall()
            return
        }
        redisCommands.keys(pattern).asFlux()
            .buffer(1000)
            .collect { keys ->
                redisCommands.unlink(*keys.toTypedArray())
            }
    }
}
