package io.stereov.singularity.auth.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.model.SecurityAlertType
import io.stereov.singularity.auth.guest.exception.model.GuestCannotPerformThisActionException
import io.stereov.singularity.auth.oauth2.util.getWellKnownProvider
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
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
class SecurityAlertService(
    private val appProperties: AppProperties,
    private val translateService: TranslateService,
    private val emailService: EmailService,
    private val templateService: TemplateService
) {

    private val logger = KotlinLogging.logger {  }
    val slug = "security_alert"

    suspend fun send(
        user: UserDocument,
        locale: Locale?,
        alertType: SecurityAlertType,
        providerKey: String? = null,
        twoFactorMethod: TwoFactorMethod? = null,
        oldEmail: String? = null,
        newEmail: String? = null,
    ) {
        logger.debug { "Sending security alert email for user ${user.id}; reason: $alertType" }

        val actualLocale = locale ?: appProperties.locale

        if (user.isGuest)
            throw GuestCannotPerformThisActionException("Failed to send verification email: a guest cannot verify an email address")

        val email = if (alertType == SecurityAlertType.EMAIL_CHANGED && oldEmail != null && newEmail != null) {
            oldEmail
        } else {
            user.sensitive.email
                ?: throw InvalidDocumentException("No email specified in user document")
        }

        val templatePath = "${EmailConstants.TEMPLATE_DIR}/$slug.html"
        val providerName = providerKey
            ?.let { getWellKnownProvider(it) }
            ?: "Unknown"
        val twoFactorMethodName = twoFactorMethod?.let { when(it) {
            TwoFactorMethod.TOTP -> "TOTP"
            TwoFactorMethod.EMAIL -> "Email"
        } } ?: "Unknown"


        val subject = translateService.translateResourceKey(TranslateKey("$slug.subject.${alertType.value}"),
            EmailConstants.RESOURCE_BUNDLE, actualLocale)
        val alert = translateService.translateResourceKey(TranslateKey("$slug.message.${alertType.value}"),
            EmailConstants.RESOURCE_BUNDLE, actualLocale)
        val content = TemplateBuilder.fromResource(templatePath)
            .translate(EmailConstants.RESOURCE_BUNDLE, actualLocale)
            .replacePlaceholders(templateService.getPlaceholders(mapOf(
                "name" to user.sensitive.name,
                "security_alert" to alert,
                "provider_name" to providerName,
                "2fa_method" to twoFactorMethodName,
                "old_email" to (oldEmail ?: "unknown"),
                "new_email" to (newEmail ?: "unknown"),
            )))
            .build()

        emailService.sendEmail(email, subject, content, actualLocale)
    }
}
