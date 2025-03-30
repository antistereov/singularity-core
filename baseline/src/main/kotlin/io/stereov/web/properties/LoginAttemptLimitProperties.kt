package io.stereov.web.properties

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
 * @property ipTimeWindowMinutes The time window in minutes for the IP address limit.
 * @property emailLimit The maximum number of login attempts allowed for a single email address.
 * @property emailTimeWindowMinutes The time window in minutes for the email address limit.
 */
@ConfigurationProperties(prefix = "baseline.security.login-attempt-limit")
data class LoginAttemptLimitProperties(
    val ipLimit: Long,
    val ipTimeWindowMinutes: Long,
    val emailLimit: Long,
    val emailTimeWindowMinutes: Long,
)
