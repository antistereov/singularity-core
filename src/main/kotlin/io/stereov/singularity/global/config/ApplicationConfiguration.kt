package io.stereov.singularity.global.config

import io.stereov.singularity.auth.properties.AuthProperties
import io.stereov.singularity.global.exception.handler.GlobalBaseWebExceptionHandler
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.global.properties.UiProperties
import io.stereov.singularity.jwt.properties.JwtProperties
import io.stereov.singularity.ratelimit.properties.LoginAttemptLimitProperties
import io.stereov.singularity.ratelimit.properties.RateLimitProperties
import io.stereov.singularity.twofactorauth.properties.TwoFactorAuthProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

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
    UiProperties::class,
)
@EnableScheduling
class ApplicationConfiguration {

    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun globalExceptionHandler() = GlobalBaseWebExceptionHandler()

}
