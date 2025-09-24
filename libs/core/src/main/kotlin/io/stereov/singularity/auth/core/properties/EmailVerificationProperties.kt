package io.stereov.singularity.auth.core.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.auth.email-verification")
data class EmailVerificationProperties(
    val uri: String = "http://localhost:8000/auth/verify-email"
)
