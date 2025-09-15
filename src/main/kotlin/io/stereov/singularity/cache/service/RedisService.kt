package io.stereov.singularity.cache.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.stereov.singularity.cache.exception.model.RedisKeyNotFoundException
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
class RedisService(
    val commands: RedisCoroutinesCommands<String, ByteArray>,
    val objectMapper: ObjectMapper
) {
    val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Saves a value to Redis with the given key.
     *
     * @param key The key to save the value under.
     * @param value The value to save.
     */
    suspend fun <T: Any> saveData(key: String, value: T) {
        logger.debug { "Saving value: $value for key: $key to Redis" }

        val string = objectMapper.writeValueAsString(value)
        commands.set(key, string.toByteArray())
    }

    /**
     * Retrieves a value from Redis by its key.
     *
     * @param key The key to retrieve the value for.
     *
     * @return The value associated with the key.
     *
     * @throws RedisKeyNotFoundException If the key is not found in Redis.
     */
    final suspend inline fun <reified T: Any> getData(key: String): T {
        logger.debug { "Getting value for key: $key" }

        return getDataOrNull(key)
            ?: throw RedisKeyNotFoundException(key)
    }

    /**
     * Retrieves a value from Redis by its key.
     *
     * @param key The key to retrieve the value for.
     *
     * @return The value associated with the key, or null if not found.
     */
    final suspend inline fun <reified T: Any>getDataOrNull(key: String): T? {
        logger.debug { "Getting value for key: $key" }

        return commands.get(key)
            ?.let { objectMapper.readValue<T>(it) }
    }

    /**
     * Deletes a value from Redis by its key.
     *
     * This method removes the specified key and its associated value from Redis.
     *
     * @param key The key to delete.
     */
    suspend fun deleteData(key: String) {
        logger.debug { "Deleting data for key: $key" }

        commands.del(key)
    }

    /**
     * Deletes all data from Redis.
     *
     * This method clears all keys and values from the Redis database.
     */
    suspend fun deleteAll() {
        commands.flushall()
    }
}
