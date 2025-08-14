package io.stereov.singularity.security.core.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.security")
data class SecurityProperties(
    val allowedOrigins: List<String> = emptyList()
)
