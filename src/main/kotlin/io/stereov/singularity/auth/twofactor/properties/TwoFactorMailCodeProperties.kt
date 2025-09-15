package io.stereov.singularity.auth.twofactor.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.auth.two-factor.mail.code")
data class TwoFactorMailCodeProperties(
    val expiresIn: Long = 60 * 15
)