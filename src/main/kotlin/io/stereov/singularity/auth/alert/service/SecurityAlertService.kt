package io.stereov.singularity.auth.alert.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapError
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.alert.exception.AlertException
import io.stereov.singularity.auth.core.model.SecurityAlertType
import io.stereov.singularity.auth.oauth2.util.getWellKnownProvider
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.email.core.exception.EmailException
import io.stereov.singularity.email.core.service.EmailService
import io.stereov.singularity.email.core.util.EmailConstants
import io.stereov.singularity.email.template.service.TemplateService
import io.stereov.singularity.email.template.util.TemplateBuilder
import io.stereov.singularity.email.template.util.build
import io.stereov.singularity.email.template.util.replacePlaceholders
import io.stereov.singularity.email.template.util.translate
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.translate.model.TranslateKey
import io.stereov.singularity.translate.service.TranslateService
import jakarta.mail.internet.MimeMessage
import org.springframework.stereotype.Service
import java.util.*

/**
 * Service responsible for sending security alert emails for various user account events,
 * such as email or password changes, updates to two-factor authentication settings,
 * and modifications to OAuth2 connections.
 */
@Service
class SecurityAlertService(
    private val appProperties: AppProperties,
    private val translateService: TranslateService,
    private val emailService: EmailService,
    private val templateService: TemplateService
) {

    private val logger = KotlinLogging.logger {  }
    val slug = "security_alert"

    /**
     * Sends a security alert email notifying about a change in the user's email address.
     *
     * @param oldEmail The previous email address associated with the user.
     * @param newEmail The new email address to which the user's account has been updated.
     * @param user The user object containing details of the account receiving the alert.
     * @param locale The locale to be used for the email content, or null to use the default locale.
     * @return A [Result] containing a [MimeMessage] if the email is sent successfully, or an [AlertException] if an error occurs.
     */
    suspend fun sendEmailChanged(
        oldEmail: String,
        newEmail: String,
        user: User,
        locale: Locale?
    ): Result<MimeMessage, AlertException> {
        return send(
            user,
            locale,
            SecurityAlertType.EMAIL_CHANGED,
            oldEmail = oldEmail,
            newEmail = newEmail
        )
    }

    /**
     * Sends a security alert email notifying the user about a password change.
     *
     * @param user The user object containing details of the account receiving the alert.
     * @param locale The locale to be used for the email content, or null to use the default locale.
     * @return A [Result] containing a [MimeMessage] if the email is sent successfully, or an [AlertException] if an error occurs.
     */
    suspend fun sendPasswordChanged(user: User, locale: Locale?): Result<MimeMessage, AlertException> {
        return send(user, locale, SecurityAlertType.PASSWORD_CHANGED)
    }

    /**
     * Sends a security alert email notifying the user about the addition of a two-factor authentication method.
     *
     * @param user The user object containing details of the account receiving the alert.
     * @param twoFactorMethod The two-factor authentication method that was added.
     * @param locale The locale to be used for the email content, or null to use the default locale.
     * @return A [Result] containing a [MimeMessage] if the email is sent successfully, or an [AlertException] if an error occurs.
     */
    suspend fun sendTwoFactorAdded(user: User, twoFactorMethod: TwoFactorMethod, locale: Locale?): Result<MimeMessage, AlertException> {
        return send(user, locale, SecurityAlertType.TWO_FACTOR_ADDED, twoFactorMethod = twoFactorMethod)
    }

    /**
     * Sends a security alert email notifying the user about the removal of a two-factor authentication method.
     *
     * @param user The user object containing details of the account receiving the alert.
     * @param twoFactorMethod The two-factor authentication method that was removed.
     * @param locale The locale to be used for the email content, or null to use the default locale.
     * @return A [Result] containing a [MimeMessage] if the email is sent successfully, or an [AlertException] if an error occurs.
     */
    suspend fun sendTwoFactorRemoved(user: User, twoFactorMethod: TwoFactorMethod, locale: Locale?): Result<MimeMessage, AlertException> {
        return send(user, locale, SecurityAlertType.TWO_FACTOR_REMOVED, twoFactorMethod = twoFactorMethod)
    }

    /**
     * Sends a security alert email notifying the user about a new OAuth2 connection.
     *
     * @param user The user object containing details of the account receiving the alert.
     * @param providerKey The key identifying the OAuth2 provider.
     * @param locale The locale to be used for the email content, or null to use the default locale.
     * @return A [Result] containing a [MimeMessage] if the email is sent successfully, or an [AlertException] if an error occurs.
     */
    suspend fun sendOAuth2Connected(user: User, providerKey: String, locale: Locale?): Result<MimeMessage, AlertException> {
        return send(user, locale, SecurityAlertType.OAUTH_CONNECTED, providerKey)
    }

    /**
     * Sends a notification to the user indicating that the OAuth2 connection has been disconnected.
     *
     * @param user The user to whom the notification will be sent.
     * @param providerKey The key identifying the OAuth2 provider that was disconnected.
     * @param locale The locale to be used for localization of the notification content, if applicable.
     * @return A [Result] containing the prepared [MimeMessage] if successful, or an [AlertException] indicating the error.
     */
    suspend fun sendOAuth2Disconnected(user: User, providerKey: String, locale: Locale?): Result<MimeMessage, AlertException> {
        return send(user, locale, SecurityAlertType.OAUTH_DISCONNECTED, providerKey)
    }

    suspend fun send(
        user: User,
        locale: Locale?,
        alertType: SecurityAlertType,
        providerKey: String? = null,
        twoFactorMethod: TwoFactorMethod? = null,
        oldEmail: String? = null,
        newEmail: String? = null,
    ): Result<MimeMessage, AlertException> = coroutineBinding {
        logger.debug { "Sending security alert email for user ${user.id}; reason: $alertType" }

        val actualLocale = locale ?: appProperties.locale

        val email = if (alertType == SecurityAlertType.EMAIL_CHANGED && oldEmail != null && newEmail != null) {
            oldEmail
        } else {
            user.sensitive.email
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
            .mapError { ex -> AlertException.Template("Failed to create template for security alert: ${ex.message}", ex) }
            .bind()

        emailService.sendEmail(email, subject, content, actualLocale)
            .mapError { when (it) {
                is EmailException.Send -> AlertException.Send("Failed to send verification email: ${it.message}", it)
                is EmailException.Disabled -> AlertException.EmailDisabled(it.message)
                is EmailException.Template -> AlertException.Template("Failed to create template for verification email: ${it.message}", it)
                is EmailException.Authentication -> AlertException.EmailAuthentication("Failed to send verification email due to an authentication failure: ${it.message}", it)
            } }
            .bind()
    }
}
