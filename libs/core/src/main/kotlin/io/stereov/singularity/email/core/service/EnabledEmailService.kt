package io.stereov.singularity.email.core.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.mapError
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.email.core.exception.EmailException
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.global.properties.AppProperties
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.MailAuthenticationException
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

    override suspend fun sendEmail(
        to: String,
        subject: String,
        content: String,
        locale: Locale
    ): Result<MimeMessage, EmailException> = coroutineBinding {
        logger.debug { "Sending email with subject \"$subject\" to \"$to\"" }

        val template = emailTemplateService.createTemplate(subject, content, locale)
            .mapError { ex -> EmailException.Template("Failed to create template: ${ex.message}", ex) }
            .bind()

        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")
        helper.setFrom(emailProperties.email, appProperties.name)
        helper.setTo(to)
        helper.setSubject(subject)
        helper.setText(template, true)

        runSuspendCatching {
            withContext(Dispatchers.IO) {
                mailSender.send(message)
            }
        }.mapError { ex -> when (ex) {
                is MailAuthenticationException -> EmailException.Authentication("Email authentication failed: ${ex.message}", ex)
                else -> EmailException.Send("Failed to send email: ${ex.message}", ex)
            }
        }.bind()

        message
    }
}
