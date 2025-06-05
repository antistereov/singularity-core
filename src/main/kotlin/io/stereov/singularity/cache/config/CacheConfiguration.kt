package io.stereov.singularity.cache.config

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.stereov.singularity.cache.exception.handler.RedisExceptionHandler
import io.stereov.singularity.cache.service.AccessTokenCache
import io.stereov.singularity.cache.service.RedisService
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.jwt.properties.JwtProperties
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
    fun accessTokenCache(
        commands: RedisCoroutinesCommands<String, ByteArray>,
        jwtProperties: JwtProperties,
    ): AccessTokenCache {
        return AccessTokenCache(commands, jwtProperties)
    }

    @Bean
    @ConditionalOnMissingBean
    fun redisService(redisCoroutinesCommands: RedisCoroutinesCommands<String, ByteArray>): RedisService {
        return RedisService(redisCoroutinesCommands)
    }

    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun redisExceptionHandler() = RedisExceptionHandler()
}
