package io.stereov.web.config.secrets

import io.stereov.web.config.ApplicationConfiguration
import io.stereov.web.global.service.secrets.component.KeyManager
import io.stereov.web.global.service.secrets.component.SecretCache
import io.stereov.web.global.service.secrets.service.EncryptionSecretService
import io.stereov.web.global.service.secrets.service.JwtSecretService
import io.stereov.web.properties.secrets.KeyManagerProperties
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
    fun jwtSecretService(keyManager: KeyManager): JwtSecretService {
        return JwtSecretService(keyManager)
    }

    @Bean
    @ConditionalOnMissingBean
    fun encryptionSecretService(keyManager: KeyManager): EncryptionSecretService {
        return EncryptionSecretService(keyManager)
    }

    @Bean
    @ConditionalOnMissingBean
    fun secretCache(keyManagerProperties: KeyManagerProperties): SecretCache {
        return SecretCache(keyManagerProperties)
    }
}
