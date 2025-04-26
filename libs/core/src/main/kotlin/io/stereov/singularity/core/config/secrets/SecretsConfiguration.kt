package io.stereov.singularity.core.config.secrets

import io.stereov.singularity.core.config.ApplicationConfiguration
import io.stereov.singularity.core.global.service.secrets.component.KeyManager
import io.stereov.singularity.core.global.service.secrets.component.SecretCache
import io.stereov.singularity.core.global.service.secrets.service.EncryptionSecretService
import io.stereov.singularity.core.global.service.secrets.service.JwtSecretService
import io.stereov.singularity.core.properties.AppProperties
import io.stereov.singularity.core.properties.secrets.KeyManagerProperties
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
@EnableConfigurationProperties(KeyManagerProperties::class)
class SecretsConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun jwtSecretService(keyManager: KeyManager, appProperties: AppProperties): JwtSecretService {
        return JwtSecretService(keyManager, appProperties)
    }

    @Bean
    @ConditionalOnMissingBean
    fun encryptionSecretService(keyManager: KeyManager, appProperties: AppProperties): EncryptionSecretService {
        return EncryptionSecretService(keyManager, appProperties)
    }

    @Bean
    @ConditionalOnMissingBean
    fun secretCache(keyManagerProperties: KeyManagerProperties): SecretCache {
        return SecretCache(keyManagerProperties)
    }
}
