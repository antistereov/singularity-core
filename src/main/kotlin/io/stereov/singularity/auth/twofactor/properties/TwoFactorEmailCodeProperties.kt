package io.stereov.singularity.auth.twofactor.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.auth.two-factor.email.code")
data class TwoFactorEmailCodeProperties(
    val expiresIn: Long = 60 * 15
)
