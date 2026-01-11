package io.stereov.singularity.cache.config

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.stereov.singularity.cache.service.CacheService
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.global.config.ApplicationConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.core.ReactiveRedisTemplate
import tools.jackson.databind.json.JsonMapper

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
        jsonMapper: JsonMapper,
        redisTemplate: ReactiveRedisTemplate<String, String>,
        emailProperties: EmailProperties
    ): CacheService {
        return CacheService(
            redisCoroutinesCommands,
            jsonMapper,
            redisTemplate,
            emailProperties
        )
    }
}
