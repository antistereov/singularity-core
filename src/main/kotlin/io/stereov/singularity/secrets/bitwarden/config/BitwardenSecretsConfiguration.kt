package io.stereov.singularity.secrets.bitwarden.config

import com.bitwarden.sdk.BitwardenClient
import com.bitwarden.sdk.BitwardenSettings
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.secrets.bitwarden.component.BitwardenSecretStore
import io.stereov.singularity.secrets.bitwarden.properties.BitwardenSecretStoreProperties
import io.stereov.singularity.secrets.core.component.SecretCache
import io.stereov.singularity.secrets.core.component.SecretStore
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
    ]
)
@EnableConfigurationProperties(BitwardenSecretStoreProperties::class)
@ConditionalOnProperty(prefix = "singularity.secrets", value = ["store"], havingValue = "bitwarden", matchIfMissing = false)
class BitwardenSecretsConfiguration {


    @Bean
    @ConditionalOnMissingBean
    fun bitwardenSecretStore(client: BitwardenClient, properties: BitwardenSecretStoreProperties, secretCache: SecretCache): SecretStore {
        return BitwardenSecretStore(client, properties, secretCache)
    }

    @Bean
    @ConditionalOnMissingBean
    fun bitwardenClient(properties: BitwardenSecretStoreProperties): BitwardenClient {
        val bitwardenSettings = BitwardenSettings(properties.apiUrl, properties.identityUrl)
        val bitwardenClient = BitwardenClient(bitwardenSettings)

        bitwardenClient.auth().loginAccessToken(properties.accessToken, properties.stateFile)

        return bitwardenClient
    }
}
