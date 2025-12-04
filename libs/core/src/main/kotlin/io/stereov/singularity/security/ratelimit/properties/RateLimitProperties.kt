package io.stereov.singularity.security.ratelimit.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.security.rate-limit")
data class RateLimitProperties(
    val ipLimit: Long = 200,
    val ipTimeWindow: Long = 1,
    val userLimit: Long = 200,
    val userTimeWindow: Long = 1,
)
