package io.stereov.web.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * # JWT properties.
 *
 * This class is responsible for holding the JWT properties
 * and is annotated with [ConfigurationProperties]
 * to bind the properties from the application configuration file.
 *
 * It is prefixed with `baseline.security.jwt` in the configuration file.
 *
 * @property secretKey The secret key used for signing the JWT tokens.
 * @property expiresIn The expiration time of the JWT tokens in seconds.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@ConfigurationProperties(prefix = "baseline.security.jwt")
data class JwtProperties(
    val secretKey: String,
    val expiresIn: Long = 900,
)
