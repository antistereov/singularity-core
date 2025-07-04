package io.stereov.singularity.secrets.config

import com.bitwarden.sdk.BitwardenClient
import com.bitwarden.sdk.BitwardenSettings
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.secrets.component.BitwardenKeyManager
import io.stereov.singularity.secrets.component.SecretCache
import io.stereov.singularity.secrets.properties.BitwardenKeyManagerProperties
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
@EnableConfigurationProperties(BitwardenKeyManagerProperties::class)
@ConditionalOnProperty(prefix = "singularity.secrets", value = ["key-manager"], havingValue = "bitwarden", matchIfMissing = false)
class BitwardenSecretsConfiguration {


    @Bean
    @ConditionalOnMissingBean
    fun bitwardenKeyManager(client: BitwardenClient, properties: BitwardenKeyManagerProperties, secretCache: SecretCache): BitwardenKeyManager {
        return BitwardenKeyManager(client, properties, secretCache)
    }

    @Bean
    @ConditionalOnMissingBean
    fun bitwardenClient(properties: BitwardenKeyManagerProperties): BitwardenClient {
        val bitwardenSettings = BitwardenSettings(properties.apiUrl, properties.identityUrl)
        val bitwardenClient = BitwardenClient(bitwardenSettings)

        bitwardenClient.auth().loginAccessToken(properties.accessToken, properties.stateFile)

        return bitwardenClient
    }
}
