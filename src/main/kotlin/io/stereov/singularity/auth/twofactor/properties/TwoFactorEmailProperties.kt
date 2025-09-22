package io.stereov.singularity.auth.twofactor.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.auth.two-factor.email")
data class TwoFactorEmailProperties(
    val enableByDefault: Boolean = true
)
