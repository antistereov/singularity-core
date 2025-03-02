package io.stereov.web.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "webstarter.backend")
data class BackendProperties(
    val baseUrl: String,
    val secure: Boolean = false,
)
