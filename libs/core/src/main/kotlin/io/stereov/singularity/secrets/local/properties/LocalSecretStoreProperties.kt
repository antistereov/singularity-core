package io.stereov.singularity.secrets.local.properties

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("singularity.secrets.sqlite")
@ConditionalOnProperty(prefix = "singularity.secrets", value = ["store"], havingValue = "local", matchIfMissing = true)
data class LocalSecretStoreProperties(
    val secretDirectory: String = "./.data/secrets"
)
