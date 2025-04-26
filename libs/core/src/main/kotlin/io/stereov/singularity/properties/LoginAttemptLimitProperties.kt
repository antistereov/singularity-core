package io.stereov.singularity.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * # Login Attempt Limit properties.
 *
 * This class is responsible for holding the login attempt limit properties
 * and is annotated with [ConfigurationProperties]
 * to bind the properties from the application configuration file.
 *
 * It is prefixed with `baseline.security.login-attempt-limit` in the configuration file.
 *
 * @property ipLimit The maximum number of login attempts allowed from a single IP address.
 * @property ipTimeWindow The time window in minutes for the IP address limit.
 */
@ConfigurationProperties(prefix = "baseline.security.login-attempt-limit")
data class LoginAttemptLimitProperties(
    val ipLimit: Long = 10,
    val ipTimeWindow: Long = 5,
)
