package io.stereov.web.config.secrets

import com.bitwarden.sdk.BitwardenClient
import com.bitwarden.sdk.BitwardenSettings
import io.stereov.web.config.ApplicationConfiguration
import io.stereov.web.global.service.secrets.component.BitwardenKeyManager
import io.stereov.web.properties.secrets.BitwardenKeyManagerProperties
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
@EnableConfigurationProperties(BitwardenKeyManagerProperties::class) // TODO: Update properties in json and README
@ConditionalOnProperty(prefix = "baseline.secrets", value = ["key-manager"], havingValue = "bitwarden", matchIfMissing = false)
class BitwardenSecretsConfiguration {


    @Bean
    @ConditionalOnMissingBean
    fun bitwardenKeyManager(client: BitwardenClient, properties: BitwardenKeyManagerProperties): BitwardenKeyManager {
        return BitwardenKeyManager(client, properties)
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
