package io.stereov.web.config

import io.stereov.web.properties.*
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

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
    FrontendProperties::class,
    JwtProperties::class,
    MailProperties::class,
    RateLimitProperties::class,
    TwoFactorAuthProperties::class
)
class ApplicationConfiguration
