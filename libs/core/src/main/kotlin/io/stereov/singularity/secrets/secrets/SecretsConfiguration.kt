package io.stereov.singularity.secrets.secrets

import io.stereov.singularity.config.ApplicationConfiguration
import io.stereov.singularity.properties.AppProperties
import io.stereov.singularity.secrets.component.KeyManager
import io.stereov.singularity.secrets.component.SecretCache
import io.stereov.singularity.secrets.properties.KeyManagerProperties
import io.stereov.singularity.secrets.service.EncryptionSecretService
import io.stereov.singularity.secrets.service.HashSecretService
import io.stereov.singularity.secrets.service.JwtSecretService
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
    fun hashSecretService(keyManager: KeyManager, appProperties: AppProperties) = HashSecretService(keyManager, appProperties)

    @Bean
    @ConditionalOnMissingBean
    fun secretCache(keyManagerProperties: KeyManagerProperties): SecretCache {
        return SecretCache(keyManagerProperties)
    }
}
