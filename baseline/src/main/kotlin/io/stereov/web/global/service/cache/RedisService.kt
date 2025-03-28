package io.stereov.web.global.service.cache

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.stereov.web.global.service.cache.exception.model.RedisKeyNotFoundException
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
    redisConnection: StatefulRedisConnection<String, ByteArray>,
) {
    private val logger: KLogger
        get() = KotlinLogging.logger {}

    private val commands: RedisCoroutinesCommands<String, ByteArray> = redisConnection.coroutines()

    /**
     * Saves a value to Redis with the given key.
     *
     * @param key The key to save the value under.
     * @param value The value to save.
     */
    suspend fun saveData(key: String, value: String) {
        logger.debug { "Saving value: $value for key: $key to Redis" }

        commands.set(key, value.toByteArray())
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
    suspend fun getData(key: String): String {
        logger.debug { "Getting value for key: $key" }

        return commands.get(key)?.let { String(it) }
            ?: throw RedisKeyNotFoundException(key)
    }

    /**
     * Retrieves a value from Redis by its key.
     *
     * @param key The key to retrieve the value for.
     *
     * @return The value associated with the key, or null if not found.
     */
    suspend fun getDataOrNull(key: String): String? {
        logger.debug { "Getting value for key: $key" }

        return commands.get(key)?.let { String(it) }
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
