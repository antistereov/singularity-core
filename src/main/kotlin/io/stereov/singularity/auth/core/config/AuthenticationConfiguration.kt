package io.stereov.singularity.auth.core.config

import io.stereov.singularity.auth.core.exception.handler.AuthExceptionHandler
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.AuthenticationService
import io.stereov.singularity.auth.core.service.CookieCreator
import io.stereov.singularity.auth.core.service.TokenValueExtractor
import io.stereov.singularity.file.s3.config.S3Configuration
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.AppProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
        S3Configuration::class,
    ]
)
@EnableConfigurationProperties(AuthProperties::class)
class AuthenticationConfiguration {

    // Services

    @Bean
    @ConditionalOnMissingBean
    fun authenticationService(): AuthenticationService {
        return AuthenticationService()
    }

    @Bean
    @ConditionalOnMissingBean
    fun cookieCreator(appProperties: AppProperties) = CookieCreator(appProperties)

    @Bean
    @ConditionalOnMissingBean
    fun tokenValueExtractor(authProperties: AuthProperties) = TokenValueExtractor(authProperties)

    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun authExceptionHandler(): AuthExceptionHandler {
        return AuthExceptionHandler()
    }
}
