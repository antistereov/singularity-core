package io.stereov.singularity.auth.core.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.auth")
data class AuthProperties(
    val publicPaths: List<String> = emptyList(),
    val userPaths: List<String> = emptyList(),
    val adminPaths: List<String> = emptyList(),
    val allowHeaderAuthentication: Boolean = true,
    val preferHeaderAuthentication: Boolean = true,
    val allowOauth2Providers: Boolean = false,
)
