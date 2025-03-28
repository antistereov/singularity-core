package io.stereov.web.properties

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * # Mail properties.
 *
 * This class is responsible for holding the mail properties
 * and is annotated with [ConfigurationProperties]
 * to bind the properties from the application configuration file.
 *
 * It is prefixed with `baseline.mail` in the configuration file and only set if
 * the `enable-verification` property is set to `true`.
 *
 * @property enableVerification Indicates whether email verification is enabled.
 * @property host The SMTP server host.
 * @property port The SMTP server port.
 * @property email The email address used for sending emails.
 * @property username The username for SMTP authentication.
 * @property password The password for SMTP authentication.
 * @property transportProtocol The transport protocol (e.g., "smtp").
 * @property smtpAuth Indicates whether SMTP authentication is enabled.
 * @property smtpStarttls Indicates whether STARTTLS is enabled.
 * @property debug Indicates whether debug mode is enabled for SMTP.
 * @property verificationExpiration The expiration time for email verification in seconds.
 * @property verificationSendCooldown The cooldown time for sending verification emails in seconds.
 * @property uiVerificationPath The UI path for email verification.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
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
    val uiVerificationPath: String,
)
