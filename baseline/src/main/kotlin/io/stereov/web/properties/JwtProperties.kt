package io.stereov.web.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "baseline.security.jwt")
data class JwtProperties(
    val secretKey: String,
    val expiresIn: Long = 900,
)
