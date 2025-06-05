package io.stereov.singularity.encryption.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.stereov.singularity.encryption.service.EncryptionService
import io.stereov.singularity.secrets.component.KeyManager
import io.stereov.singularity.secrets.config.BitwardenSecretsConfiguration
import io.stereov.singularity.secrets.config.SecretsConfiguration
import io.stereov.singularity.secrets.service.EncryptionSecretService
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
    fun encryptionService(encryptionSecretService: EncryptionSecretService, keyManager: KeyManager, objectMapper: ObjectMapper): EncryptionService {
        return EncryptionService(encryptionSecretService, keyManager, objectMapper)
    }
}
