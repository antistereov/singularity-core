package io.stereov.singularity.encryption.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.stereov.singularity.encryption.service.EncryptionSecretService
import io.stereov.singularity.encryption.service.EncryptionService
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.secrets.core.component.SecretStore
import io.stereov.singularity.secrets.bitwarden.config.BitwardenSecretsConfiguration
import io.stereov.singularity.secrets.core.config.SecretsConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@AutoConfiguration(
    after = [
        SecretsConfiguration::class,
        BitwardenSecretsConfiguration::class,
    ]
)
class EncryptionConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun encryptionSecretService(secretStore: SecretStore, appProperties: AppProperties): EncryptionSecretService {
        return EncryptionSecretService(secretStore, appProperties)
    }

    @Bean
    @ConditionalOnMissingBean
    fun encryptionService(encryptionSecretService: EncryptionSecretService, secretStore: SecretStore, objectMapper: ObjectMapper): EncryptionService {
        return EncryptionService(encryptionSecretService, secretStore, objectMapper)
    }
}
