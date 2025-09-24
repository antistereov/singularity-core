package io.stereov.singularity.ratelimit.config

import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import io.stereov.singularity.auth.core.config.AuthenticationConfiguration
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.ratelimit.excpetion.handler.RateLimitExceptionHandler
import io.stereov.singularity.ratelimit.properties.LoginAttemptLimitProperties
import io.stereov.singularity.ratelimit.properties.RateLimitProperties
import io.stereov.singularity.ratelimit.service.RateLimitService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
        AuthenticationConfiguration::class
    ]
)
@EnableConfigurationProperties(
    LoginAttemptLimitProperties::class,
    RateLimitProperties::class
)
class RateLimitConfiguration {

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun rateLimitService(
        authorizationService: AuthorizationService,
        proxyManager: LettuceBasedProxyManager<String>,
        rateLimitProperties: RateLimitProperties,
        loginAttemptLimitProperties: LoginAttemptLimitProperties,
    ): RateLimitService {
        return RateLimitService(authorizationService, proxyManager, rateLimitProperties, loginAttemptLimitProperties)
    }

    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun rateLimitExceptionHandler() = RateLimitExceptionHandler()


}
