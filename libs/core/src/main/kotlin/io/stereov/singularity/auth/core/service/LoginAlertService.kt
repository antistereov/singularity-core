package io.stereov.singularity.auth.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.model.SessionInfo
import io.stereov.singularity.auth.guest.exception.model.GuestCannotPerformThisActionException
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
class LoginAlertService(
    private val appProperties: AppProperties,
    private val translateService: TranslateService,
    private val emailService: EmailService,
    private val templateService: TemplateService
) {

    private val logger = KotlinLogging.logger {  }

    suspend fun send(user: UserDocument, locale: Locale?, session: SessionInfo) {
        logger.debug { "Sending login alert email for user ${user.id}" }

        val actualLocale = locale ?: appProperties.locale

        if (user.isGuest)
            throw GuestCannotPerformThisActionException("Failed to send verification email: a guest cannot verify an email address")

        val email = user.sensitive.email
            ?: throw InvalidDocumentException("No email specified in user document")

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
                "login_device" to device,
                "login_browser" to browser
            )))
            .build()

        emailService.sendEmail(email, subject, content, actualLocale)
    }
}