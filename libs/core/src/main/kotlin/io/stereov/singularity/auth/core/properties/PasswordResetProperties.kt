package io.stereov.singularity.auth.core.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.auth.password-reset")
data class PasswordResetProperties(
    val uri: String = "http://localhost:8000/auth/reset-password"
)
