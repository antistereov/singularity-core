package io.stereov.web.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "webstarter.frontend")
data class FrontendProperties(
    val baseUrl: String,
    val emailVerificationPath: String,
)
