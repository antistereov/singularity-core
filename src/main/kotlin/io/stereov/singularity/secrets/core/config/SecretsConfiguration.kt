package io.stereov.singularity.secrets.core.config

import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.secrets.core.component.SecretCache
import io.stereov.singularity.secrets.core.properties.SecretStoreProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
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

    @Bean
    @ConditionalOnMissingBean
    fun secretCache(secretStoreProperties: SecretStoreProperties): SecretCache {
        return SecretCache(secretStoreProperties)
    }
}
