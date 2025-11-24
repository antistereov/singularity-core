package io.stereov.singularity.secrets.core.config

import io.stereov.singularity.cache.service.CacheService
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.secrets.core.component.SecretCache
import io.stereov.singularity.secrets.core.controller.SecretRotationController
import io.stereov.singularity.secrets.core.properties.SecretStoreProperties
import io.stereov.singularity.secrets.core.service.SecretRotationService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
    ]
)
@EnableConfigurationProperties(SecretStoreProperties::class)
class SecretsConfiguration {

    // Component

    @Bean
    @ConditionalOnMissingBean
    fun secretCache(
        secretStoreProperties: SecretStoreProperties,
        cacheService: CacheService
    ): SecretCache {
        return SecretCache(secretStoreProperties, cacheService)
    }

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun secretRotationController(
        secretRotationService: SecretRotationService
    ) = SecretRotationController(
        secretRotationService,
        authorizationService
    )

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun secretsRotationService(
        context: ApplicationContext
    ) = SecretRotationService(
        context
    )
}
