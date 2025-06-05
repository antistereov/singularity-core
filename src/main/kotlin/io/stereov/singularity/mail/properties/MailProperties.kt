package io.stereov.singularity.mail.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.mail")
data class MailProperties(
    val host: String,
    val port: Int = 0,
    val email: String,
    val username: String,
    val password: String,
    val transportProtocol: String = "smtp",
    val smtpAuth: Boolean = true,
    val smtpStarttls: Boolean = true,
    val debug: Boolean = false,
    val verificationExpiration: Long = 900,
    val verificationSendCooldown: Long = 60,
    val passwordResetExpiration: Long = 900,
    val passwordResetSendCooldown: Long = 60,
    val uiVerificationPath: String = "/auth/verify-email",
    val uiPasswordResetPath: String = "/auth/reset-password",
)
