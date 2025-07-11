package io.stereov.singularity.cache.config

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import io.stereov.singularity.cache.service.RedisService
import io.stereov.singularity.global.config.ApplicationConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * # Configuration class for Redis-related beans.
 *
 * This class is responsible for configuring the Redis-related services
 * and components in the application.
 *
 * It runs after the [MongoReactiveAutoConfiguration], [SpringDataWebAutoConfiguration],
 * [RedisAutoConfiguration], and [ApplicationConfiguration] classes to ensure that
 * the necessary configurations are applied in the correct order.
 *
 * It enables the following services:
 * - [RedisService]
 *
 * It enables the following beans:
 * - [StatefulRedisConnection]
 * - [RedisClient]
 * - [LettuceBasedProxyManager]
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
    ]
)
@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun redisClient(redisProperties: RedisProperties): RedisClient {
        val redisUri = RedisURI.builder()
            .withHost(redisProperties.host)
            .withPort(redisProperties.port)
            .withSsl(redisProperties.ssl.isEnabled)
            .apply {
                if (!redisProperties.password.isNullOrEmpty()) {
                    withPassword(redisProperties.password?.toCharArray())
                }
                withDatabase(redisProperties.database)
            }
            .build()

        return RedisClient.create(redisUri)
    }

    @Bean
    @ConditionalOnMissingBean
    fun statefulRedisConnection(redisClient: RedisClient): StatefulRedisConnection<String, ByteArray> {
        return redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec()))
    }

    @Bean
    @ConditionalOnMissingBean
    fun bucketProxyManager(connection: StatefulRedisConnection<String, ByteArray>): LettuceBasedProxyManager<String> {
        return Bucket4jLettuce.casBasedBuilder(connection)
            .expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10)))
            .build()
    }

    @Bean
    @ConditionalOnMissingBean
    fun redisCoroutinesCommands(connection: StatefulRedisConnection<String, ByteArray>): RedisCoroutinesCommands<String, ByteArray> {
        return connection.coroutines()
    }

    @Bean
    @ConditionalOnMissingBean
    fun redisService(commands: RedisCoroutinesCommands<String, ByteArray>): RedisService {
        return RedisService(commands)
    }
}
