package io.stereov.web.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * # Encryption properties.
 *
 * This class is responsible for holding the encryption properties
 * and is annotated with [ConfigurationProperties]
 * to bind the properties from the application configuration file.
 *
 * It is prefixed with `baseline.security.encryption` in the configuration file.
 *
 * @property secretKey The secret key used for encryption and decryption.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@ConfigurationProperties(prefix = "baseline.security.encryption")
data class EncryptionProperties(
    val secretKey: String,
)
