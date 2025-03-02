package io.stereov.web.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "webstarter.encryption")
data class EncryptionProperties(
    val secretKey: String,
)
