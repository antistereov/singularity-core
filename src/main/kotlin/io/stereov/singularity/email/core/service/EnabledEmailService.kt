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

/**
 * Service implementation of [EmailService] responsible for sending emails when email functionality
 * is enabled in the application configuration.
 *
 * This service provides an email-sending mechanism using the [JavaMailSender] for constructing
 * and delivering emails. It integrates with other services like [EmailTemplateService], to generate
 * localized email templates, and uses properties defined in [EmailProperties] and [AppProperties]
 * to configure email settings and application metadata.
 *
 * The service is conditionally loaded based on the `singularity.email.enable` property in the
 * application configuration. When this property is set to `true`, the service is activated and ready
 * to handle email operations. If set to `false`, the service is not loaded, [FailingEmailService] will be used.
 *
 * Key responsibilities of this service include:
 * - Constructing and preparing the email content and metadata.
 * - Generating localized email templates through the [EmailTemplateService].
 * - Handling exceptions related to email sending, such as authentication failures or sending errors.
 */
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
