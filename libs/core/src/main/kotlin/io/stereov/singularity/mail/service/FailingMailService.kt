package io.stereov.singularity.mail.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.mail.exception.model.MailDisabledException
import io.stereov.singularity.content.translate.model.Language
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(prefix = "singularity.app", value = ["enable-mail"], havingValue = "false", matchIfMissing = true)
class FailingMailService() : MailService {

    override val logger = KotlinLogging.logger {}

    override suspend fun sendEmail(to: String, subject: String, content: String, lang: Language) {
        logger.warn { "Cannot send email: mail is disabled in configuration" }

        throw MailDisabledException()
    }
}
