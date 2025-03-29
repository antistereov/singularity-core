package io.stereov.web.global.service.mail

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.service.mail.exception.MailCooldownException
import io.stereov.web.properties.MailProperties
import io.stereov.web.properties.UiProperties
import io.stereov.web.user.model.UserDocument
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

/**
 * # Service for sending emails.
 *
 * This service provides methods to send verification and password reset emails.
 * It uses the [JavaMailSender] to send emails and the [MailTokenService] to create tokens for
 * email verification and password resets.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Service
@ConditionalOnProperty(prefix = "baseline.mail", name = ["enable"], havingValue = "true", matchIfMissing = false)
class MailService(
    private val mailSender: JavaMailSender,
    private val mailProperties: MailProperties,
    private val uiProperties: UiProperties,
    private val mailCooldownService: MailCooldownService,
    private val mailTokenService: MailTokenService,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Sends a verification email to the user.
     *
     * @param user The user to send the verification email to.
    */
    suspend fun sendVerificationEmail(user: UserDocument) {
        logger.debug { "Sending verification email to ${user.email}" }

        val userId = user.idX

        if (mailCooldownService.getRemainingVerificationCooldown(userId) > 0) {
            throw MailCooldownException(mailCooldownService.getRemainingVerificationCooldown(userId))
        }

        val token = mailTokenService.createVerificationToken(user.email, user.security.mail.verificationSecret)
        val verificationUrl = generateVerificationUrl(token)
        val message = SimpleMailMessage()
        message.from = mailProperties.email
        message.setTo(user.email)
        message.subject = "Email Verification"
        message.text = "Hey ${user.name}! Please verify your email by clicking on the following link: $verificationUrl"

        mailSender.send(message)
        mailCooldownService.startVerificationCooldown(userId)
    }

    /**
     * Generates a verification URL for the user.
     *
     * @param token The token to include in the verification URL.
     * @return The generated verification URL.
     */
    private fun generateVerificationUrl(token: String): String {
        return "${uiProperties.baseUrl}${mailProperties.uiVerificationPath}?token=$token"
    }

    /**
     * Sends a password reset email to the user.
     *
     * @param user The user to send the password reset email to.
     */
    suspend fun sendPasswordResetEmail(user: UserDocument) {
        logger.debug { "Sending password reset email to ${user.email}" }

        val userId = user.idX

        if (mailCooldownService.getRemainingPasswordResetCooldown(userId) > 0) {
            throw MailCooldownException(mailCooldownService.getRemainingPasswordResetCooldown(userId))
        }

        val token = mailTokenService.createPasswordResetToken(user.idX, user.security.mail.passwordResetSecret)
        val passwordResetUrl = generatePasswordResetUrl(token)
        val message = SimpleMailMessage()
        message.from = mailProperties.email
        message.setTo(user.email)
        message.subject = "Password Reset"
        message.text = "Hey ${user.name}! You can reset your password by clicking on the following link: $passwordResetUrl"

        mailSender.send(message)
        mailCooldownService.startPasswordResetCooldown(userId)
    }

    /**
     * Generates a password reset URL for the user.
     *
     * @param token The token to include in the password reset URL.
     * @return The generated password reset URL.
     */
    private fun generatePasswordResetUrl(token: String): String {
        return "${uiProperties.baseUrl}${mailProperties.uiPasswordResetPath}?token=$token"
    }
}
