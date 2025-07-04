package io.stereov.singularity.auth.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.auth")
data class AuthProperties(
    val publicPaths: List<String> = emptyList(),
    val userPaths: List<String> = emptyList(),
    val adminPaths: List<String> = emptyList(),
)
