package io.stereov.web.global.service.cache

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import org.springframework.stereotype.Service

@Service
class RedisService(
    redisConnection: StatefulRedisConnection<String, ByteArray>,
) {
    private val logger: KLogger
        get() = KotlinLogging.logger {}

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    private val commands: RedisCoroutinesCommands<String, ByteArray> = redisConnection.coroutines()

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    suspend fun saveData(key: String, value: String) {
        logger.debug { "Saving value: $value for key: $key to Redis" }

        commands.set(key, value.toByteArray())
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    suspend fun getData(key: String): String? {
        logger.debug { "Getting value for key: $key" }

        return commands.get(key)?.let { String(it) }
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    suspend fun deleteData(key: String) {
        logger.debug { "Deleting data for key: $key" }

        commands.del(key)
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    suspend fun deleteAll() {
        commands.flushall()
    }
}
