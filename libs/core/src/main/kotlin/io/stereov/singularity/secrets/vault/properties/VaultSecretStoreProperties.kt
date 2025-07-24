package io.stereov.singularity.secrets.vault.properties

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("singularity.secrets.vault")
@ConditionalOnProperty(prefix = "singularity.secrets", value = ["store"], havingValue = "vault", matchIfMissing = false)
data class VaultSecretStoreProperties(
    val scheme: String = "http",
    val host: String = "localhost",
    val port: Int = 8200,
    val token: String,
    val engine: String = "apps"
)
