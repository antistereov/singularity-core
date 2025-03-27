package io.stereov.web.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "baseline.security.two-factor")
data class TwoFactorAuthProperties(
    val recoveryCodeLength: Int
)
