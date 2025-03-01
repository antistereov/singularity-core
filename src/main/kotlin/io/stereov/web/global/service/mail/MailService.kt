package io.stereov.web.global.service.mail

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.service.jwt.JwtService
import io.stereov.web.properties.FrontendProperties
import io.stereov.web.properties.MailProperties
import io.stereov.web.user.model.UserDocument
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import javax.security.auth.login.AccountException

@Service
class MailService(
    private val mailSender: JavaMailSender,
    private val mailProperties: MailProperties,
    private val frontendProperties: FrontendProperties,
    private val jwtService: JwtService,
    private val mailVerificationCooldownService: MailVerificationCooldownService,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun sendVerificationEmail(user: UserDocument) {
        logger.debug { "Sending verification email to ${user.email}" }

        val userId = user.id ?: throw AccountException("UserDocument does not contain an ID")

        val token = jwtService.createEmailVerificationToken(user.email, user.verificationUuid)
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
        return "${frontendProperties.baseUrl}${frontendProperties.emailVerificationPath}?token=$token"
    }
}
