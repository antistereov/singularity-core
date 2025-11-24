package io.stereov.singularity.email.core.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
import io.stereov.singularity.email.core.util.EmailConstants
import io.stereov.singularity.email.template.exception.TemplateException
import io.stereov.singularity.email.template.service.TemplateService
import io.stereov.singularity.email.template.util.TemplateBuilder
import org.springframework.stereotype.Service
import java.util.*

/**
 * Service responsible for creating localized email templates by processing subjects, content,
 * and dynamic placeholders.
 *
 * This service integrates with the [TemplateService] for obtaining predefined and custom placeholders
 * and handles the operations to load, translate, process, and construct the final email template. It
 * ensures localization and placeholder replacement based on the given parameters.
 *
 * The service is designed to build templates using a base template resource and dynamically includes
 * subject, content, and other application-specific placeholders.
 */
@Service
class EmailTemplateService(
    private val templateService: TemplateService
) {

    /**
     * Creates an email template by processing the given subject and content with localization
     * and placeholder replacement.
     *
     * The method retrieves predefined and additional placeholders, translates the base template
     * using the specified locale, replaces the placeholders, and builds the final template content.
     *
     * @param subject The subject content of the email template.
     * @param content The email template body content.
     * @param locale The locale to be used for translating the template content.
     * @return A [Result] that contains either the final rendered template as a [String] if successful,
     * or an instance of [TemplateException] if an error occurs during template processing.
     */
    suspend fun createTemplate(subject: String, content: String, locale: Locale): Result<String, TemplateException> {
        val placeholders = templateService.getPlaceholders(mapOf(
            "subject" to subject,
            "content" to content
        ))

        return TemplateBuilder.fromResource(EmailConstants.BASE_TEMPLATE_PATH)
            .andThen { it.translate(EmailConstants.RESOURCE_BUNDLE, locale) }
            .map { it.replacePlaceholders(placeholders) }
            .map { it.build() }
    }

}
