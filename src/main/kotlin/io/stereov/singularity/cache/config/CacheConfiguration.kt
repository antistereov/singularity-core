package io.stereov.singularity.cache.config

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
class CacheConfiguration {

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun redisService(
        jsonMapper: JsonMapper,
        redisTemplate: ReactiveRedisTemplate<String, String>,
        emailProperties: EmailProperties
    ): CacheService {
        return CacheService(
            jsonMapper,
            redisTemplate,
            emailProperties
        )
    }
}
