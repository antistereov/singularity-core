package io.stereov.web.properties

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "baseline.mail")
@ConditionalOnProperty(prefix = "baseline.mail", name = ["enable-verification"], havingValue = "true", matchIfMissing = false)
data class MailProperties(
    val enableVerification: Boolean = false,
    val host: String,
    val port: Int = 0,
    val email: String,
    val username: String,
    val password: String,
    val transportProtocol: String,
    val smtpAuth: Boolean = false,
    val smtpStarttls: Boolean = false,
    val debug: Boolean = false,
    val verificationExpiration: Long = 900,
    val verificationSendCooldown: Long = 60,
)
