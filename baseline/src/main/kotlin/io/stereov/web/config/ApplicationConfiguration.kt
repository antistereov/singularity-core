package io.stereov.web.config

import io.stereov.web.global.service.file.service.LocalFileStorage
import io.stereov.web.properties.*
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * # Application configuration class.
 *
 * This class is responsible for configuring the application properties and enabling
 * configuration properties for various components.
 *
 * It runs after the [MongoReactiveAutoConfiguration], [SpringDataWebAutoConfiguration]
 * and [RedisAutoConfiguration] classes to ensure that the necessary configurations are
 * applied in the correct order.
 *
 * This class enables the following configuration properties:
 * - [AppProperties]
 * - [AuthProperties]
 * - [EncryptionProperties]
 * - [UiProperties]
 * - [JwtProperties]
 * - [RateLimitProperties]
 * - [TwoFactorAuthProperties]
 * - [LoginAttemptLimitProperties]
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Configuration
@AutoConfiguration(
    after = [
        MongoReactiveAutoConfiguration::class,
        SpringDataWebAutoConfiguration::class,
        RedisAutoConfiguration::class,
    ]
)
@EnableConfigurationProperties(
    AppProperties::class,
    AuthProperties::class,
    EncryptionProperties::class,
    FileStorageProperties::class,
    JwtProperties::class,
    LocalFileStorageProperties::class,
    LoginAttemptLimitProperties::class,
    RateLimitProperties::class,
    TwoFactorAuthProperties::class,
    UiProperties::class,
)
class ApplicationConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "baseline.file.storage", name = ["type"], havingValue = "local", matchIfMissing = false)
    fun fileStorage(fileStorageProperties: LocalFileStorageProperties): LocalFileStorage {
        return LocalFileStorage(fileStorageProperties)
    }
}
