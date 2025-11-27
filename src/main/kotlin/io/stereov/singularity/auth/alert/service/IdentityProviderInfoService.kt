package io.stereov.singularity.auth.alert.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.alert.component.ProviderStringCreator
import io.stereov.singularity.auth.alert.exception.AlertException
import io.stereov.singularity.auth.core.service.PasswordResetService
import io.stereov.singularity.cache.service.CacheService
import io.stereov.singularity.email.core.exception.EmailException
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
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.translate.model.TranslateKey
import io.stereov.singularity.translate.service.TranslateService
import jakarta.mail.internet.MimeMessage
import org.springframework.stereotype.Service
import java.util.*

/**
 * This service is responsible for sending emails to users with information regarding identity providers.
 * It integrates cooldown mechanisms, localized content generation, and email sending functionalities.
 * This service ensures users do not receive duplicate emails during a cooldown period and uses template-based
 * content generation for emails tailored to the recipient's language settings.
 *
 * Features:
 * - Checks cooldown period before sending an email to ensure emails are not sent repeatedly in a short interval.
 * - Uses translation and localization services to generate email content in the appropriate language.
 * - Utilizes templates for email content creation, dynamically replacing placeholders with recipient-specific values.
 * - Sends the email to the user and manages cooldown settings post-sending.
 * - Handles errors gracefully, including scenarios like email sending failures, template generation issues,
 *   and cooldown setting errors.
 */
@Service
class IdentityProviderInfoService(
    private val appProperties: AppProperties,
    private val translateService: TranslateService,
    private val emailService: EmailService,
    private val templateService: TemplateService,
    private val providerStringCreator: ProviderStringCreator,
    private val passwortResetService: PasswordResetService,
    override val cacheService: CacheService,
    override val emailProperties: EmailProperties
) : CooldownEmailService {

    override val logger = KotlinLogging.logger {  }
    override val slug = "identity_provider_info"

    /**
     * Sends an email with information about identity providers to a user. This includes
     * checking for an active cooldown period to prevent duplicate emails, creating the email content
     * with a localized template, and sending the email using the email service.
     *
     * @param user The user document containing details about the recipient.
     * @param locale Optional, the locale to use for translating and formatting the email content.
     *               Defaults to the application's configuration if not provided.
     * @return A [Result] object containing either the successfully sent [MimeMessage] or an [AlertException]
     *         in the event of a failure.
     */
    suspend fun send(
        user: User,
        locale: Locale?,
    ): Result<MimeMessage, AlertException> = coroutineBinding {
        logger.debug { "Sending no identity provider info email to user ${user.id}" }

        val email = user.email

        val cooldownActive = isCooldownActive(email)
            .mapError { ex -> AlertException.CooldownCache("Failed to check cooldown for identity provider info: ${ex.message}", ex) }
            .bind()

        if (cooldownActive) {
            logger.debug { "Skipping sending of email because cooldown is still active" }
            Err(AlertException.CooldownActive("Skipping sending of email because cooldown is still active")).bind()
        }

        val actualLocale = locale ?: appProperties.locale
        val subject = translateService.translateResourceKey(
            TranslateKey("$slug.subject"),
            EmailConstants.RESOURCE_BUNDLE, actualLocale)
        val templatePath = "${EmailConstants.TEMPLATE_DIR}/$slug.html"

        val content = TemplateBuilder.fromResource(templatePath)
            .translate(EmailConstants.RESOURCE_BUNDLE, actualLocale)
            .replacePlaceholders(
                templateService.getPlaceholders(
                    mapOf(
                        "name" to user.sensitive.name,
                        "provider_placeholder" to providerStringCreator.getProvidersString(
                            user,
                            actualLocale
                        ),
                        "reset_password_uri" to passwortResetService.generatePasswordResetUri(user)
                    )
                ))
            .build()
            .mapError { ex -> AlertException.Template("Failed to create template for identity provider info: ${ex.message}", ex) }
            .bind()

        emailService.sendEmail(email, subject, content, actualLocale)
            .mapError { when (it) {
                is EmailException.Send -> AlertException.Send("Failed to send verification email: ${it.message}", it)
                is EmailException.Disabled -> AlertException.EmailDisabled(it.message)
                is EmailException.Template -> AlertException.Template("Failed to create template for verification email: ${it.message}", it)
                is EmailException.Authentication -> AlertException.EmailAuthentication("Failed to send verification email due to an authentication failure: ${it.message}", it)
            } }
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
