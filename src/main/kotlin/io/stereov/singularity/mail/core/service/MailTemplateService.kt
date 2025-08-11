package io.stereov.singularity.mail.core.service

import io.stereov.singularity.mail.core.util.MailConstants
import io.stereov.singularity.mail.template.service.TemplateService
import io.stereov.singularity.mail.template.util.TemplateBuilder
import io.stereov.singularity.content.translate.model.Language
import org.springframework.stereotype.Service

@Service
class MailTemplateService(
    private val templateService: TemplateService
) {

    suspend fun createTemplate(subject: String, content: String, lang: Language): String {
        val placeholders = templateService.getPlaceholders(mapOf(
            "subject" to subject,
            "content" to content
        ))

        return TemplateBuilder.fromResource(MailConstants.BASE_TEMPLATE_PATH)
            .translate(MailConstants.RESOURCE_BUNDLE, lang)
            .replacePlaceholders(placeholders)
            .build()
    }

}
