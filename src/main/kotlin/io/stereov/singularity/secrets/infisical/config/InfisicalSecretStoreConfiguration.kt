package io.stereov.singularity.secrets.infisical.config

import com.infisical.sdk.InfisicalSdk
import com.infisical.sdk.config.SdkConfig
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.secrets.core.component.SecretCache
import io.stereov.singularity.secrets.infisical.component.InfisicalSecretStore
import io.stereov.singularity.secrets.infisical.properties.InfisicalSecretStoreProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.json.JsonMapper


@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
    ]
)
@EnableConfigurationProperties(InfisicalSecretStoreProperties::class)
@ConditionalOnProperty(prefix = "singularity.secrets", value = ["store"], havingValue = "infisical", matchIfMissing = false)
class InfisicalSecretStoreConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun infisicalSecretStore(
        infisicalProperties: InfisicalSecretStoreProperties,
        secretCache: SecretCache,
        infisical: InfisicalSdk,
        jsonMapper: JsonMapper
    ) = InfisicalSecretStore(
        infisicalProperties,
        secretCache,
        infisical,
        jsonMapper
    )

    @Bean
    @ConditionalOnMissingBean
    fun infisicalSdk(
        properties: InfisicalSecretStoreProperties
    ): InfisicalSdk {
        val sdk = InfisicalSdk(
            SdkConfig.Builder()
                .withSiteUrl(properties.url)
                .build()
        )

        sdk.Auth().UniversalAuthLogin(
            properties.clientId,
            properties.clientSecret
        )

        return sdk
    }
}
