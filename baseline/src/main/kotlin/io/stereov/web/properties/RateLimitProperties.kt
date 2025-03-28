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
 * @property ipRateLimitMinute The rate limit for IP addresses in requests per minute.
 * @property accountRateLimitMinute The rate limit for accounts in requests per minute.
 * @property ipRateLimitRefreshMinute The refresh rate limit for IP addresses in minutes.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@ConfigurationProperties(prefix = "baseline.security.rate-limit")
data class RateLimitProperties(
    val ipRateLimitMinute: Long,
    val accountRateLimitMinute: Long = 200,
    val ipRateLimitRefreshMinute: Long = 1,
)
