package io.stereov.web.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "baseline.frontend")
data class FrontendProperties(
    val baseUrl: String,
    val emailVerificationPath: String,
)
