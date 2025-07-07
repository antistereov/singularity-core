package io.stereov.singularity.secrets.hashicorp.config

import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.secrets.hashicorp.properties.HashiCorpKeyManagerProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.vault.authentication.ClientAuthentication
import org.springframework.vault.authentication.TokenAuthentication
import org.springframework.vault.client.VaultEndpoint
import org.springframework.vault.config.AbstractReactiveVaultConfiguration

@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
    ]
)
@EnableConfigurationProperties(HashiCorpKeyManagerProperties::class)
@ConditionalOnProperty(prefix = "singularity.secrets", value = ["key-manager"], havingValue = "hashicorp", matchIfMissing = false)
class HashiCorpVaultConfiguration(
    private val properties: HashiCorpKeyManagerProperties
) : AbstractReactiveVaultConfiguration()  {


    override fun vaultEndpoint(): VaultEndpoint {
        return VaultEndpoint.create(properties.host, properties.port)
    }

    override fun clientAuthentication(): ClientAuthentication {
        return TokenAuthentication(properties.token)
    }
}