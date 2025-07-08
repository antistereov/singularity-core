package io.stereov.singularity.hash.config

import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.hash.service.HashSecretService
import io.stereov.singularity.hash.service.HashService
import io.stereov.singularity.secrets.core.component.SecretStore
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

@AutoConfiguration
class HashConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun hashSecretService(secretStore: SecretStore, appProperties: AppProperties) = HashSecretService(secretStore, appProperties)

    @Bean
    @ConditionalOnMissingBean
    fun hashService(hashSecretService: HashSecretService): HashService {
        return HashService(hashSecretService)
    }
}
