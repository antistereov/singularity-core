package io.stereov.singularity.secrets.vault.config

import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.secrets.core.component.SecretStore
import io.stereov.singularity.secrets.vault.component.VaultSecretStore
import io.stereov.singularity.secrets.vault.properties.VaultSecretStoreProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.vault.authentication.ClientAuthentication
import org.springframework.vault.authentication.TokenAuthentication
import org.springframework.vault.client.VaultEndpoint
import org.springframework.vault.config.AbstractReactiveVaultConfiguration
import org.springframework.vault.core.ReactiveVaultOperations

@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
    ]
)
@EnableConfigurationProperties(VaultSecretStoreProperties::class)
@ConditionalOnProperty(prefix = "singularity.secrets", value = ["store"], havingValue = "vault", matchIfMissing = false)
class VaultSecretStoreConfiguration(
    private val properties: VaultSecretStoreProperties
) : AbstractReactiveVaultConfiguration()  {

    @Bean
    @ConditionalOnMissingBean
    fun vaultSecretStore(properties: VaultSecretStoreProperties, appProperties: AppProperties, vaultOperations: ReactiveVaultOperations): SecretStore {
        return VaultSecretStore(properties, appProperties, vaultOperations)
    }

    override fun vaultEndpoint(): VaultEndpoint {
        return VaultEndpoint.create(properties.host, properties.port)
    }

    override fun clientAuthentication(): ClientAuthentication {
        return TokenAuthentication(properties.token)
    }
}
