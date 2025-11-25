package io.stereov.singularity.auth.alert.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapError
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.alert.exception.AlertException
import io.stereov.singularity.auth.core.model.SessionInfo
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
 * Service responsible for sending login alert emails to users. It leverages
 * translation and templating features to customize the email content
 * based on user-specific information such as login details and locale.
 */
@Service
class LoginAlertService(
    private val appProperties: AppProperties,
    private val translateService: TranslateService,
    private val emailService: EmailService,
    private val templateService: TemplateService
) {

    private val logger = KotlinLogging.logger {  }

    /**
     * Sends a login alert email to the specified user.
     *
     * @param user The user document containing the user's information.
     * @param locale The optional locale to translate the email content; defaults to application properties locale if null.
     * @param session The session information, including details about the user's login.
     * @return A [Result] containing the sent [MimeMessage] on success or an [AlertException] on failure.
     */
    suspend fun send(
        user: UserDocument,
        locale: Locale?, session: SessionInfo
    ): Result<MimeMessage, AlertException> = coroutineBinding {
        logger.debug { "Sending login alert email for user ${user.id}" }

        val email = user.email
        val actualLocale = locale ?: appProperties.locale

        val slug = "login_alert"
        val templatePath = "${EmailConstants.TEMPLATE_DIR}/$slug.html"

        val unknownMessage = translateService.translateResourceKey(
            TranslateKey("$slug.details.unknown"),
            EmailConstants.RESOURCE_BUNDLE,
            actualLocale
        )
        val loginLocation = if (session.location?.cityName != null) {
            "${session.location.cityName}, ${session.location.countryCode}"
        } else { unknownMessage }
        val device = session.os ?: unknownMessage
        val browser = session.browser ?: unknownMessage

        val subject = translateService.translateResourceKey(
            TranslateKey("$slug.subject"),
            EmailConstants.RESOURCE_BUNDLE, actualLocale)
        val content = TemplateBuilder.fromResource(templatePath)
            .translate(EmailConstants.RESOURCE_BUNDLE, actualLocale)
            .replacePlaceholders(templateService.getPlaceholders(mapOf(
                "name" to user.sensitive.name,
                "login_location" to loginLocation,
                "login_os" to device,
                "login_browser" to browser
            )))
            .build()
            .mapError { ex -> AlertException.Template("Failed to create template for login alert: ${ex.message}", ex) }
            .bind()

        emailService.sendEmail(email, subject, content, actualLocale)
            .mapError { ex -> AlertException.Send("Failed to send login alert: ${ex.message}", ex) }
            .bind()
    }
}