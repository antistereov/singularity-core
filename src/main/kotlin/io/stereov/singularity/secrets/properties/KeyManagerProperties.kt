package io.stereov.singularity.secrets.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("singularity.secrets")
data class KeyManagerProperties(
    val keyManager: KeyManagerImplementation = KeyManagerImplementation.Bitwarden,
    val keyRotationCron: String = "0 0 4 1 1,4,7,10 *",
    val cacheExpiration: Long = 900000,
)
