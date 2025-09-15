package io.stereov.singularity.auth.twofactor.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.auth.two-factor.totp.recovery-code")
data class TotpRecoveryCodeProperties(
    val length: Int = 10,
    val count: Int = 6,
)
