package io.stereov.singularity.email.core.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.email.core.exception.EmailException
import jakarta.mail.internet.MimeMessage
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.*

@Service
@ConditionalOnProperty(prefix = "singularity.email", value = ["enable"], havingValue = "false", matchIfMissing = true)
class FailingEmailService() : EmailService {

    override val logger = KotlinLogging.logger {}

    override suspend fun sendEmail(to: String, subject: String, content: String, locale: Locale): Result<MimeMessage, EmailException.Disabled> {
        logger.warn { "Cannot send email: email is disabled in configuration" }

        return Err(EmailException.Disabled("Action cannot be performed: email is disabled in configuration"))
    }
}
