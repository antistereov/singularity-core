package io.stereov.singularity.cache.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.stereov.singularity.cache.exception.CacheException
import kotlinx.coroutines.reactive.collect
import kotlinx.coroutines.reactor.asFlux
import org.springframework.stereotype.Service

/**
 * Service for managing Redis cache operations.
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
     * Checks if a value saved on the given key exists.
     *
     * @param key The key.
     *
     * @return True if a value on this key exists, of false if not.
     */
    suspend fun exists(key: String): Result<Boolean, CacheException.Operation> {
        logger.debug { "Checking if value for key $key exists" }

        return runCatching {  redisCommands.exists(key)?.let { it > 0 } ?: false }
            .mapError { ex -> CacheException.Operation("Failed to check existence of key $key: ${ex.message}", ex) }
    }

    /**
     * Retrieves a value from Redis by its key.
     *
     * @param key The key to retrieve the value for.
     * @param T The type of the value.
     *
     * @return The value associated with the key.
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
     * Deletes one or more keys.
     *
     * This method removes the specified key and its associated value from Redis.
     *
     * @param keys The keys to delete.
     *
     * @return The number of deleted keys.
     */
    suspend fun delete(vararg keys: String): Result<Long, CacheException.Operation> {
        logger.debug { "Deleting data for keys: $keys" }

        return runCatching { redisCommands.unlink(*keys) }
            .mapError { ex -> CacheException.Operation("Failed to delete keys ${keys}: ${ex.message}", ex) }
            .map { it ?: 0 }
    }

    /**
     * Deletes all data from Redis.
     *
     * This method clears all keys and values from the Redis database.
     *
     * @param pattern Optional. Delete only keys matching the pattern if present or flush the cache if null.
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
