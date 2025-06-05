package io.stereov.singularity.user.service.mail

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.global.properties.UiProperties
import io.stereov.singularity.mail.exception.model.MailCooldownException
import io.stereov.singularity.mail.service.MailService
import io.stereov.singularity.mail.util.MailConstants
import io.stereov.singularity.template.service.TemplateService
import io.stereov.singularity.template.util.TemplateBuilder
import io.stereov.singularity.translate.model.Language
import io.stereov.singularity.translate.model.TranslateKey
import io.stereov.singularity.translate.service.TranslateService
import io.stereov.singularity.user.model.UserDocument
import org.springframework.stereotype.Component

@Component
class UserMailSender(
    private val mailCooldownService: MailCooldownService,
    private val mailTokenService: MailTokenService,
    private val uiProperties: UiProperties,
    private val translateService: TranslateService,
    private val mailService: MailService,
    private val templateService: TemplateService
)  {

    private val logger = KotlinLogging.logger {}

    /**
     * Generates a verification URL for the user.
     *
     * @param token The token to include in the verification URL.
     * @return The generated verification URL.
     */
    private fun generateVerificationUrl(token: String): String {
        return "${uiProperties.baseUrl}${uiProperties.emailVerificationPath}?token=$token"
    }

    /**
     * Sends a verification email to the user.
     *
     * @param user The user to send the verification email to.
     */
    suspend fun sendVerificationEmail(user: UserDocument, lang: Language, newEmail: String? = null) {
        logger.debug { "Sending verification email to ${newEmail ?: user.sensitive.email}" }

        val userId = user.id

        val remainingCooldown = mailCooldownService.getRemainingVerificationCooldown(userId)
        if (remainingCooldown > 0) {
            throw MailCooldownException(remainingCooldown)
        }

        val secret = user.sensitive.security.mail.verificationSecret

        val email = newEmail ?: user.sensitive.email
        val token = mailTokenService.createVerificationToken(userId, email, secret)
        val verificationUrl = generateVerificationUrl(token)

        val slug = "email_verification"
        val templatePath = "${MailConstants.TEMPLATE_DIR}/$slug.html"

        val subject = translateService.translate(TranslateKey("$slug.subject"), MailConstants.RESOURCE_BUNDLE, lang)
        val content = TemplateBuilder.fromResource(templatePath)
            .translate(MailConstants.RESOURCE_BUNDLE, lang)
            .replacePlaceholders(templateService.getPlaceholders(mapOf(
                "name" to user.sensitive.name,
                "verification_url" to verificationUrl
            )))
            .build()

        mailService.sendEmail(email, subject, content, lang)
        mailCooldownService.startVerificationCooldown(userId)
    }



    /**
     * Sends a password reset email to the user.
     *
     * @param user The user to send the password reset email to.
     */
    suspend fun sendPasswordResetEmail(user: UserDocument, lang: Language) {
        logger.debug { "Sending password reset email to ${user.sensitive.email}" }

        val userId = user.id

        val remainingCooldown = mailCooldownService.getRemainingPasswordResetCooldown(userId)
        if (remainingCooldown > 0) {
            throw MailCooldownException(remainingCooldown)
        }

        val secret = user.sensitive.security.mail.passwordResetSecret

        val token = mailTokenService.createPasswordResetToken(user.id, secret)
        val passwordResetUrl = generatePasswordResetUrl(token)

        val slug = "password_reset"
        val templatePath = "${MailConstants.TEMPLATE_DIR}/$slug.html"

        val subject = translateService.translate(TranslateKey("$slug.subject"), MailConstants.RESOURCE_BUNDLE, lang)
        val content = TemplateBuilder.fromResource(templatePath)
            .translate(MailConstants.RESOURCE_BUNDLE, lang)
            .replacePlaceholders(templateService.getPlaceholders(mapOf(
                "name" to user.sensitive.name,
                "reset_url" to passwordResetUrl
            )))
            .build()

        mailService.sendEmail(user.sensitive.email, subject, content, lang)
        mailCooldownService.startPasswordResetCooldown(userId)
    }

    /**
     * Generates a password reset URL for the user.
     *
     * @param token The token to include in the password reset URL.
     * @return The generated password reset URL.
     */
    private fun generatePasswordResetUrl(token: String): String {
        return "${uiProperties.baseUrl}${uiProperties.passwordResetPath}?token=$token"
    }

}
