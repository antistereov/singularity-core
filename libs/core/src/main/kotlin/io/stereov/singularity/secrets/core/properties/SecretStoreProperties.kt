package io.stereov.singularity.secrets.core.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("singularity.secrets")
data class SecretStoreProperties(
    val store: SecretStoreImplementation = SecretStoreImplementation.LOCAL,
    val keyRotationCron: String = "0 0 4 1 1,4,7,10 *",
    val cacheExpiration: Long = 900000,
)
