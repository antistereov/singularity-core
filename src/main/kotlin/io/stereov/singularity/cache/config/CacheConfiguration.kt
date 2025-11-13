package io.stereov.singularity.cache.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.stereov.singularity.cache.service.CacheService
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
    ): CacheService {
        return CacheService(redisCoroutinesCommands, objectMapper)
    }
}
