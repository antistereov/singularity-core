package io.stereov.singularity.mail.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.global.properties.UiProperties
import io.stereov.singularity.mail.properties.MailProperties
import io.stereov.singularity.template.service.TemplateService
import io.stereov.singularity.translate.model.Language
import io.stereov.singularity.user.service.mail.MailCooldownService
import io.stereov.singularity.user.service.mail.MailTokenService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
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
class MailService(
    private val mailSender: JavaMailSender,
    private val mailProperties: MailProperties,
    private val uiProperties: UiProperties,
    private val mailCooldownService: MailCooldownService,
    private val mailTokenService: MailTokenService,
    private val mailTemplateService: MailTemplateService,
) {

    private val logger: KLogger = KotlinLogging.logger {}

    private val sendMailScope = CoroutineScope(Dispatchers.Default)

    suspend fun sendEmail(
        to: String,
        subject: String,
        content: String,
        lang: Language
    ) {
        logger.debug { "Sending email with subject \"$subject\" to \"$to\"" }

        val message = mailSender.createMimeMessage()
        val template = mailTemplateService.createTemplate(subject, content, lang)

        val helper = MimeMessageHelper(message, true, "UTF-8")
        helper.setFrom(mailProperties.email)
        helper.setTo(to)
        helper.setSubject(subject)
        helper.setText(template, true)

        sendMailScope.launch { mailSender.send(message) }
    }
}
