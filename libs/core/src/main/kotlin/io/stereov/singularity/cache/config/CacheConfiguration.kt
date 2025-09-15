package io.stereov.singularity.cache.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.stereov.singularity.cache.exception.handler.RedisExceptionHandler
import io.stereov.singularity.cache.service.RedisService
import io.stereov.singularity.global.config.ApplicationConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
        RedisConfiguration::class
    ]
)
@OptIn(ExperimentalLettuceCoroutinesApi::class)
class CacheConfiguration {

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun redisService(
        redisCoroutinesCommands: RedisCoroutinesCommands<String, ByteArray>,
        objectMapper: ObjectMapper
    ): RedisService {
        return RedisService(redisCoroutinesCommands, objectMapper)
    }

    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun redisExceptionHandler() = RedisExceptionHandler()
}
