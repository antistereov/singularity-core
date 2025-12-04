package io.stereov.singularity.auth.alert.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.alert.exception.AlertException
import io.stereov.singularity.auth.core.model.NoAccountInfoAction
import io.stereov.singularity.cache.service.CacheService
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.email.core.service.CooldownEmailService
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
import jakarta.mail.internet.MimeMessage
import org.springframework.stereotype.Service
import java.util.*

/**
 * Service responsible for handling and sending notification emails related to accounts with no associated information.
 * This service implements email sending with built-in cooldown mechanisms to prevent consecutive emails being sent in short intervals.
 *
 * Primary responsibilities:
 * - Translating and constructing email content using templates and translation services.
 * - Validating whether an email can be sent based on a cooldown period.
 * - Sending the constructed email to the intended recipient.
 */
@Service
class NoAccountInfoService(
    private val appProperties: AppProperties,
    private val translateService: TranslateService,
    private val emailService: EmailService,
    private val templateService: TemplateService,
    override val cacheService: CacheService,
    override val emailProperties: EmailProperties
) : CooldownEmailService {

    override val logger = KotlinLogging.logger {  }
    override val slug = "no_account_info"

    /**
     * Sends an email to notify about actions related to accounts with no associated information.
     *
     * @param email The email address of the recipient.
     * @param action The type of action for which the information email is sent (e.g., "password_reset").
     * @param locale The optional locale to be used for email content translation. If null, a default locale is used.
     * @return A [Result] containing the sent [MimeMessage] on success or an [AlertException] on failure.
     */
    suspend fun send(
        email: String,
        action: NoAccountInfoAction,
        locale: Locale?,
    ): Result<MimeMessage, AlertException> = coroutineBinding {
        logger.debug { "Sending no account info email to ${email}; reason: $action" }

        val cooldownActive = isCooldownActive(email)
            .mapError { ex -> AlertException.CooldownCache("Failed to check cooldown for identity provider info: ${ex.message}", ex) }
            .bind()

        if (cooldownActive) {
            logger.debug { "Skipping sending of email because cooldown is still active" }
            Err(AlertException.CooldownActive("Skipping sending of email because cooldown is still active")).bind()
        }

        val actualLocale = locale ?: appProperties.locale
        val templatePath = "${EmailConstants.TEMPLATE_DIR}/$slug.html"

        val actionSubject = translateService.translateResourceKey(
            TranslateKey("$slug.action_subject.${action.value}"),
            EmailConstants.RESOURCE_BUNDLE, actualLocale)
        val subject = translateService.translateResourceKey(
            TranslateKey("$slug.subject"),
            EmailConstants.RESOURCE_BUNDLE, actualLocale)
            .replace("{{ action_subject }}", actionSubject)

        val action = translateService.translateResourceKey(
            TranslateKey("$slug.action.${action.value}"),
            EmailConstants.RESOURCE_BUNDLE, actualLocale)

        val content = TemplateBuilder.fromResource(templatePath)
            .translate(EmailConstants.RESOURCE_BUNDLE, actualLocale)
            .replacePlaceholders(templateService.getPlaceholders(mapOf(
                "action" to action,
            )))
            .build()
            .mapError { ex -> AlertException.Template("Failed to create template for no account info: ${ex.message}", ex) }
            .bind()

        emailService.sendEmail(email, subject, content, actualLocale)
            .mapError { AlertException.from(it) }
            .flatMapBoth(
                success = { mimeMessage ->
                    startCooldown(email)
                        .mapError { ex -> AlertException.CooldownCache("Failed to set cooldown for identity provider info: ${ex.message}", ex) }
                        .map { mimeMessage }
                },
                failure = { ex ->
                    startCooldown(email)
                        .flatMap { Err(ex) }
                        .mapError { ex }
                }
            )
            .bind()
    }
}
