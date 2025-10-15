package io.stereov.singularity.email.core.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.global.properties.AppProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.util.*

@Service
@ConditionalOnProperty(prefix = "singularity.email", value = ["enable"], havingValue = "true", matchIfMissing = false)
class EnabledEmailService(
    private val mailSender: JavaMailSender,
    private val emailProperties: EmailProperties,
    private val emailTemplateService: EmailTemplateService,
    private val appProperties: AppProperties,
) : EmailService {

    override val logger: KLogger = KotlinLogging.logger {}

    private val sendMailScope = CoroutineScope(Dispatchers.Default)

    override suspend fun sendEmail(
        to: String,
        subject: String,
        content: String,
        locale: Locale
    ) {
        logger.debug { "Sending email with subject \"$subject\" to \"$to\"" }

        val message = mailSender.createMimeMessage()
        val template = emailTemplateService.createTemplate(subject, content, locale)

        val helper = MimeMessageHelper(message, true, "UTF-8")
        helper.setFrom(emailProperties.email, appProperties.name)
        helper.setTo(to)
        helper.setSubject(subject)
        helper.setText(template, true)

        sendMailScope.launch { mailSender.send(message) }
    }
}
