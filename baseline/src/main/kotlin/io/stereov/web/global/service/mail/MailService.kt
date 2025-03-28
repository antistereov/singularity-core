package io.stereov.web.global.service.mail

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.properties.UiProperties
import io.stereov.web.properties.MailProperties
import io.stereov.web.user.model.UserDocument
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(prefix = "baseline.mail", name = ["enable-verification"], havingValue = "true", matchIfMissing = false)
class MailService(
    private val mailSender: JavaMailSender,
    private val mailProperties: MailProperties,
    private val uiProperties: UiProperties,
    private val mailVerificationCooldownService: MailVerificationCooldownService,
    private val mailTokenService: MailTokenService,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun sendVerificationEmail(user: UserDocument) {
        logger.debug { "Sending verification email to ${user.email}" }

        val userId = user.idX

        val token = mailTokenService.createToken(user.email, user.security.mail.verificationUuid)
        val verificationUrl = generateVerificationUrl(token)
        val message = SimpleMailMessage()
        message.from = mailProperties.email
        message.setTo(user.email)
        message.subject = "Email Verification"
        message.text = "Hey ${user.name}! Please verify your email by clicking on the following link: $verificationUrl"

        mailSender.send(message)
        mailVerificationCooldownService.startEmailVerificationCooldown(userId)
    }

    private fun generateVerificationUrl(token: String): String {
        return "${uiProperties.baseUrl}${mailProperties.uiVerificationPath}?token=$token"
    }
}
