package io.stereov.web.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "baseline.security.rate-limit")
data class RateLimitProperties(
    val ipRateLimitMinute: Long,
    val accountRateLimitMinute: Long = 200,
    val ipRateLimitRefreshMinute: Long = 1,
)
