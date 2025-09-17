package io.stereov.singularity.email.core.service

import io.stereov.singularity.email.core.util.EmailConstants
import io.stereov.singularity.email.template.service.TemplateService
import io.stereov.singularity.email.template.util.TemplateBuilder
import org.springframework.stereotype.Service
import java.util.*

@Service
class EmailTemplateService(
    private val templateService: TemplateService
) {

    suspend fun createTemplate(subject: String, content: String, locale: Locale): String {
        val placeholders = templateService.getPlaceholders(mapOf(
            "subject" to subject,
            "content" to content
        ))

        return TemplateBuilder.fromResource(EmailConstants.BASE_TEMPLATE_PATH)
            .translate(EmailConstants.RESOURCE_BUNDLE, locale)
            .replacePlaceholders(placeholders)
            .build()
    }

}
