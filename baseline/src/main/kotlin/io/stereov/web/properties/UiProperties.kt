package io.stereov.web.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "baseline.ui")
data class UiProperties(
    val baseUrl: String,
)
