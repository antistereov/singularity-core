package io.stereov.singularity.auth.alert.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.auth.security-alert")
data class SecurityAlertProperties(
    val login: Boolean = true,
    val emailChanged: Boolean = true,
    val passwordChanged: Boolean = true,
    val twoFactorAdded: Boolean = true,
    val twoFactorRemoved: Boolean = true,
    val oauth2ProviderConnected: Boolean = true,
    val oauth2ProviderDisconnected: Boolean = true,
    val registrationWithExistingEmail: Boolean = true,
)