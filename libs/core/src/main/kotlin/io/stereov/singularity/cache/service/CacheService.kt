package io.stereov.singularity.cache.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KotlinLogging
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.stereov.singularity.cache.exception.CacheException
import kotlinx.coroutines.reactive.collect
import kotlinx.coroutines.reactor.asFlux
import org.springframework.stereotype.Service

/**
 * A service for interacting with a Redis cache using coroutine-based commands.
 *
 * This service provides functionality to store, retrieve, check, and delete cached data in Redis.
 * It utilizes an object mapper for serialization/deserialization and supports error handling via custom exceptions.
 *
 * @property redisCommands The Redis coroutine commands interface used for communication with the Redis server.
 * @property objectMapper The object mapper instance used for serializing and deserializing objects to/from JSON.
 */
@Service
@OptIn(ExperimentalLettuceCoroutinesApi::class)
class CacheService(
    val redisCommands: RedisCoroutinesCommands<String, ByteArray>,
    val objectMapper: ObjectMapper
) {
    val logger = KotlinLogging.logger {}

    /**
     * Saves a key-value pair in Redis with an optional expiration time.
     *
     * If the expiration time is provided, the key will automatically expire after the specified time.
     *
     * @param key The key under which the value is to be stored.
     * @param value The value to store, must be serializable.
     * @param expiresIn Optional. The time-to-live for the key in seconds. If null, the key will not expire.
     * @return A [Result] containing the original value if the operation was successful, or a [CacheException] if an error occurred.
     */
    suspend fun <T: Any> put(key: String, value: T, expiresIn: Long? = null): Result<T, CacheException> =
        coroutineBinding {
            logger.debug { "Saving key $key to Redis" }

            val string = runCatching {
                objectMapper.writeValueAsString(value)
            }
                .mapError { ex -> CacheException.ObjectMapper("Failed to write value as string: ${ex.message}", ex) }
                .bind()

            if (expiresIn != null) {
                runCatching {
                    redisCommands.setex(key, expiresIn, string.toByteArray())
                    value
                }
                    .mapError { ex -> CacheException.Operation("Failed to set key $key with expiration: ${ex.message}", ex) }
                    .bind()
            } else {
                runCatching {
                    redisCommands.set(key, string.toByteArray())
                    value
                }
                    .mapError { ex -> CacheException.Operation("Failed to set key: ${ex.message}", ex) }
                    .bind()
            }
        }

    /**
     * Checks if a value exists in Redis for the given key.
     *
     * @param key The key to check for existence in the Redis cache.
     * @return A Result containing `true` if the key exists, `false` if it does not,
     *   or a [CacheException.Operation] if an error occurs.
     */
    suspend fun exists(key: String): Result<Boolean, CacheException.Operation> {
        logger.debug { "Checking if value for key $key exists" }

        return runCatching {  redisCommands.exists(key)?.let { it > 0 } ?: false }
            .mapError { ex -> CacheException.Operation("Failed to check existence of key $key: ${ex.message}", ex) }
    }

    /**
     * Retrieves a value associated with the given key from the Redis cache.
     *
     * This function attempts to fetch the value as a specified type and deserialize it
     * using the object mapper. If the key does not exist, a [CacheException.KeyNotFound]
     * exception is returned. If the value cannot be deserialized to the specified type,
     * a [CacheException.ObjectMapper] exception is returned.
     *
     * @param T The expected type of the value to retrieve.
     * @param key The key whose associated value is to be retrieved.
     * @return A [Result] containing the value of type [T] if the operation is successful,
     *   or a [CacheException] if an error occurs.
     */
    final suspend inline fun <reified T: Any> get(key: String): Result<T, CacheException> {
        logger.debug { "Getting value for key: $key" }

        return runCatching { redisCommands.get(key) }
            .mapError { ex -> CacheException.Operation("Failed to get value for key $key: ${ex.message}", ex) }
            .andThen { value ->
                if (value != null) {
                    runCatching { objectMapper.readValue<T>(value) }
                        .mapError { ex -> CacheException.ObjectMapper("Failed to serialize value $value to class ${T::class.simpleName}: ${ex.message}", ex) }
                } else {
                    Err(CacheException.KeyNotFound("No key $key cached"))
                }
            }
    }

    /**
     * Deletes the specified keys from the Redis cache.
     *
     * This method attempts to remove the key-value pairs associated with the given keys
     * from the Redis server. If any errors occur during the operation, a [CacheException.Operation] is returned.
     *
     * @param keys The keys to be deleted from the Redis cache.
     * @return A [Result] containing the number of keys that were successfully deleted, or
     *   a [CacheException.Operation] if an error occurs during the deletion process.
     */
    suspend fun delete(vararg keys: String): Result<Long, CacheException.Operation> {
        logger.debug { "Deleting data for keys: $keys" }

        return runCatching { redisCommands.unlink(*keys) }
            .mapError { ex -> CacheException.Operation("Failed to delete keys ${keys}: ${ex.message}", ex) }
            .map { it ?: 0 }
    }

    /**
     * Deletes all keys from the Redis cache, optionally matching a specified pattern.
     *
     * If no pattern is provided, the entire cache will be flushed. If a pattern is provided,
     * only the keys matching the given pattern will be removed.
     *
     * @param pattern An optional pattern used to match the keys to be deleted. If null, all keys will be deleted.
     * @return A [Result] object indicating the success or failure of the operation. On failure, it contains
     *   a [CacheException.Operation] providing details about the error.
     */
    suspend fun deleteAll(pattern: String? = null): Result<Unit, CacheException.Operation> {
        return if (pattern == null) {
            runCatching { redisCommands.flushall() }
                .mapError { ex -> CacheException.Operation("Failed to flush cache: ${ex.message}", ex) }
        } else {
            runCatching {
                redisCommands.keys(pattern).asFlux()
                .buffer(1000)
                .collect { keys ->
                    redisCommands.unlink(*keys.toTypedArray())
                }
            }
                .mapError { ex -> CacheException.Operation("Failed to delete keys with pattern $pattern: ${ex.message}", ex) }
        }
            .map {  }

    }
}
