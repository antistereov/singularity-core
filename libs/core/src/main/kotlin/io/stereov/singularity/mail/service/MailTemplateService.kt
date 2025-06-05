package io.stereov.singularity.mail.service

import io.stereov.singularity.mail.util.MailConstants
import io.stereov.singularity.template.util.TemplateBuilder
import org.springframework.stereotype.Service

@Service
class MailTemplateService {

    suspend fun createTemplate(subject: String, content: String): String {
        val placeholders = mapOf(
            "subject" to subject,
            "content" to content
        )

        return TemplateBuilder.fromResource(MailConstants.BASE_TEMPLATE_PATH)
            .replacePlaceholders(placeholders)
            .build()
    }

}
