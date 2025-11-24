package io.stereov.singularity.security.ratelimit.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.security.login-attempt-limit")
data class LoginAttemptLimitProperties(
    val ipLimit: Long = 10,
    val ipTimeWindow: Long = 5,
)
