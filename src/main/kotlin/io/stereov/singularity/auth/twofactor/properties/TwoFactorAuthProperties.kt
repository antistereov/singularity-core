package io.stereov.singularity.auth.twofactor.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.auth.two-factor")
data class TwoFactorAuthProperties(
    val recoveryCodeLength: Int = 10,
    val recoveryCodeCount: Int = 6,
    val mailTwoFactorCodeExpiresIn: Long = 60 * 15
)
