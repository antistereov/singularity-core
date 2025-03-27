package io.stereov.web.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "baseline.security.encryption")
data class EncryptionProperties(
    val secretKey: String,
)
