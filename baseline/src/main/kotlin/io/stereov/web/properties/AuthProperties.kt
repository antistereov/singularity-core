package io.stereov.web.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "webstarter.auth")
data class AuthProperties(
    val publicPaths: List<String> = emptyList(),
    val userPaths: List<String> = emptyList(),
    val adminPaths: List<String> = emptyList(),
)
