package io.stereov.singularity.auth.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.component.ProviderStringCreator
import io.stereov.singularity.email.core.service.EmailService
import io.stereov.singularity.email.core.util.EmailConstants
import io.stereov.singularity.email.template.service.TemplateService
import io.stereov.singularity.email.template.util.TemplateBuilder
import io.stereov.singularity.global.exception.model.InvalidDocumentException
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.translate.model.TranslateKey
import io.stereov.singularity.translate.service.TranslateService
import io.stereov.singularity.user.core.model.UserDocument
import org.springframework.stereotype.Service
import java.util.*

@Service
class RegistrationAlertService(
    private val translateService: TranslateService,
    private val templateService: TemplateService,
    private val appProperties: AppProperties,
    private val emailService: EmailService,
    private val providerStringCreator: ProviderStringCreator
) {

    private val logger = KotlinLogging.logger {}
    private val slug = "registration_alert"
    private val templatePath = "${EmailConstants.TEMPLATE_DIR}/$slug.html"

    suspend fun send(user: UserDocument, locale: Locale?) {
        logger.debug { "Sending registration alert email for user ${user.id}" }

        val actualLocale = locale ?: appProperties.locale

        val email = user.sensitive.email ?: throw InvalidDocumentException("No email is set for user with ID ${user.id}")

        val subject = translateService.translateResourceKey(TranslateKey("$slug.subject"),
            EmailConstants.RESOURCE_BUNDLE, actualLocale)
        val content = TemplateBuilder.fromResource(templatePath)
            .translate(EmailConstants.RESOURCE_BUNDLE, actualLocale)
            .replacePlaceholders(templateService.getPlaceholders(mapOf(
                "name" to user.sensitive.name,
                "provider_placeholder" to providerStringCreator.getProvidersString(user, actualLocale)
            )))
            .build()

        emailService.sendEmail(email, subject, content, actualLocale)
    }
}
