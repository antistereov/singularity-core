package io.stereov.singularity.auth.jwt.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.security.jwt")
data class JwtProperties(
    val expiresIn: Long = 900,
)
