package io.stereov.singularity.content.core.config

import io.stereov.singularity.auth.core.config.AuthenticationConfiguration
import io.stereov.singularity.auth.core.service.AuthenticationService
import io.stereov.singularity.content.core.component.AccessCriteria
import io.stereov.singularity.content.core.exception.handler.ContentExceptionHandler
import io.stereov.singularity.content.core.properties.ContentProperties
import io.stereov.singularity.mail.core.config.MailConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@AutoConfiguration(
    after = [
        MongoReactiveAutoConfiguration::class,
        SpringDataWebAutoConfiguration::class,
        RedisAutoConfiguration::class,
        AuthenticationConfiguration::class,
        MailConfiguration::class
    ]
)
@EnableConfigurationProperties(ContentProperties::class)
class ContentConfiguration {

    // Component
    
    @Bean
    @ConditionalOnMissingBean
    fun accessCriteria(authenticationService: AuthenticationService): AccessCriteria {
        return AccessCriteria(authenticationService)
    }

    // ExceptionHandler

    @Bean
    @ConditionalOnMissingBean
    fun contentExceptionHandler() = ContentExceptionHandler()
}
