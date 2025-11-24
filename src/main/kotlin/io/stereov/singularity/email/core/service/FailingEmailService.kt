package io.stereov.singularity.email.core.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.email.core.exception.EmailException
import jakarta.mail.internet.MimeMessage
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.*

/**
 * A service implementation that fails to send emails when email functionality is disabled.
 *
 * This class implements the [EmailService] interface and is activated when the application
 * configuration disables email functionality. It always provides a failure response when attempting
 * to send emails, indicating that the action cannot be performed.
 *
 * This implementation is marked as a Spring service and is conditionally loaded based on the
 * `singularity.email.enable` property. If the property is set to `false` or not defined, this service
 * will be used.
 *
 * The service logs a warning message each time an email send operation is attempted, to notify
 * that the email functionality is intentionally disabled in the configuration.
 */
@Service
@ConditionalOnProperty(prefix = "singularity.email", value = ["enable"], havingValue = "false", matchIfMissing = true)
class FailingEmailService() : EmailService {

    override val logger = KotlinLogging.logger {}

    override suspend fun sendEmail(to: String, subject: String, content: String, locale: Locale): Result<MimeMessage, EmailException.Disabled> {
        logger.warn { "Cannot send email: email is disabled in configuration" }

        return Err(EmailException.Disabled("Action cannot be performed: email is disabled in configuration"))
    }
}
