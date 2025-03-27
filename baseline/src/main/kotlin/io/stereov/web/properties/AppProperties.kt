package io.stereov.web.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "baseline.app")
data class AppProperties(
    val name: String,
    val baseUrl: String,
    val secure: Boolean = false,
)
