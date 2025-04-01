package io.stereov.web.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * # Rate Limit properties.
 *
 * This class is responsible for holding the rate limit properties
 * and is annotated with [ConfigurationProperties]
 * to bind the properties from the application configuration file.
 *
 * It is prefixed with `baseline.security.rate-limit` in the configuration file.
 *
 * @property ipLimit The rate limit for IP addresses in requests per minute.
 * @property ipTimeWindow The time window for IP rate limiting in minutes.
 * @property userLimit The rate limit for users in requests per minute.
 * @property userTimeWindow The time window for user rate limiting in minutes.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@ConfigurationProperties(prefix = "baseline.security.rate-limit")
data class RateLimitProperties(
    val ipLimit: Long = 200,
    val ipTimeWindow: Long = 1,
    val userLimit: Long = 200,
    val userTimeWindow: Long = 1,
)
