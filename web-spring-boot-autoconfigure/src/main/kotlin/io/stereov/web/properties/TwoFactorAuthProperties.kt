package io.stereov.web.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "webstarter.security.two-factor")
data class TwoFactorAuthProperties(
    val recoveryCodeLength: Int
)
