package io.stereov.singularity.auth.alert.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapError
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.alert.component.ProviderStringCreator
import io.stereov.singularity.auth.alert.exception.AlertException
import io.stereov.singularity.email.core.service.EmailService
import io.stereov.singularity.email.core.util.EmailConstants
import io.stereov.singularity.email.template.service.TemplateService
import io.stereov.singularity.email.template.util.TemplateBuilder
import io.stereov.singularity.email.template.util.build
import io.stereov.singularity.email.template.util.replacePlaceholders
import io.stereov.singularity.email.template.util.translate
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.translate.model.TranslateKey
import io.stereov.singularity.translate.service.TranslateService
import io.stereov.singularity.user.core.model.UserDocument
import jakarta.mail.internet.MimeMessage
import org.springframework.stereotype.Service
import java.util.*

/**
 * Service responsible for sending registration alert emails.
 *
 * This service manages the creation and sending of emails that notify users about specific registration-related events.
 * It utilizes localized content, email templates, and placeholders to create a personalized email for the recipient.
 */
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

    /**
     * Sends a registration alert email to the specified recipient.
     *
     * @param email The email address of the recipient.
     * @param user The user document containing information about the user for whom the alert is being sent.
     * @param locale The locale to use for translating email content. If null, a default application locale will be used.
     * @return A [Result] wrapping either the sent [MimeMessage] on success or an [AlertException] on failure.
     */
    suspend fun send(
        email: String,
        user: UserDocument,
        locale: Locale?
    ): Result<MimeMessage, AlertException> = coroutineBinding {
        logger.debug { "Sending registration alert email for user ${user.id}" }

        val actualLocale = locale ?: appProperties.locale

        val subject = translateService.translateResourceKey(TranslateKey("$slug.subject"),
            EmailConstants.RESOURCE_BUNDLE, actualLocale)
        val content = TemplateBuilder.fromResource(templatePath)
            .translate(EmailConstants.RESOURCE_BUNDLE, actualLocale)
            .replacePlaceholders(templateService.getPlaceholders(mapOf(
                "name" to user.sensitive.name,
                "provider_placeholder" to providerStringCreator.getProvidersString(user, actualLocale)
            )))
            .build()
            .mapError { ex -> AlertException.Template("Failed to create template for registration alert: ${ex.message}", ex) }
            .bind()

        emailService.sendEmail(email, subject, content, actualLocale)
            .mapError { ex -> AlertException.Send("Failed to send registration alert: ${ex.message}", ex) }
            .bind()
    }
}
