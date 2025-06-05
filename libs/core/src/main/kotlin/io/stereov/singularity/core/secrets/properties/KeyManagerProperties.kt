package io.stereov.singularity.core.secrets.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Properties for configuring the key manager.
 *
 * @param keyManager The secret key manager to use.
 * @param keyRotationCron The cron string that schedules automated key rotation.
 * @param cacheExpiration The duration it takes for cached secrets to expired in milliseconds.
 */
@ConfigurationProperties("baseline.secrets")
data class KeyManagerProperties(
    val keyManager: KeyManagerImplementation = KeyManagerImplementation.Bitwarden,
    val keyRotationCron: String = "",
    val cacheExpiration: Long = 900000,
)
