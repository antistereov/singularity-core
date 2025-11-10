package io.stereov.singularity.auth.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.component.ProviderStringCreator
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.email.core.service.EmailService
import io.stereov.singularity.email.core.util.EmailConstants
import io.stereov.singularity.email.template.service.TemplateService
import io.stereov.singularity.email.template.util.TemplateBuilder
import io.stereov.singularity.global.exception.model.InvalidDocumentException
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.translate.model.TranslateKey
import io.stereov.singularity.translate.service.TranslateService
import io.stereov.singularity.user.core.model.UserDocument
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.*

@Service
class IdentityProviderInfoService(
    private val appProperties: AppProperties,
    private val translateService: TranslateService,
    private val emailService: EmailService,
    private val templateService: TemplateService,
    private val providerStringCreator: ProviderStringCreator,
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val emailProperties: EmailProperties,
    private val passwortResetService: PasswordResetService
) {

    private val logger = KotlinLogging.logger {  }
    private val slug = "identity_provider_info"

    suspend fun send(
        user: UserDocument,
        locale: Locale?,
    ) {
        logger.debug { "Sending no identity provider info email to user ${user.id}" }

        val email = user.sensitive.email ?: throw InvalidDocumentException("No email is set for user with ID ${user.id}")

        if (cooldownActive(email)) {
            logger.debug { "Skipping sending of email because cooldown is still active" }
            return
        }

        val actualLocale = locale ?: appProperties.locale
        val subject = translateService.translateResourceKey(TranslateKey("$slug.subject"),
            EmailConstants.RESOURCE_BUNDLE, actualLocale)
        val templatePath = "${EmailConstants.TEMPLATE_DIR}/$slug.html"

        val content = TemplateBuilder.fromResource(templatePath)
            .translate(EmailConstants.RESOURCE_BUNDLE, actualLocale)
            .replacePlaceholders(templateService.getPlaceholders(mapOf(
                "name" to user.sensitive.name,
                "provider_placeholder" to providerStringCreator.getProvidersString(user, actualLocale),
                "reset_password_uri" to passwortResetService.generatePasswordResetUri(user)
            )))
            .build()

        emailService.sendEmail(email, subject, content, actualLocale)
        startCooldown(email)
    }

    private suspend fun cooldownActive(email: String): Boolean {
        logger.debug { "Getting remaining cooldown for identity provider info" }

        val key = "$slug:$email"
        val remainingTtl = redisTemplate.getExpire(key).awaitSingleOrNull() ?: Duration.ofSeconds(-1)

        return (remainingTtl.seconds > 0)
    }

    private suspend fun startCooldown(email: String): Boolean {
        logger.debug { "Starting cooldown for identity provider info" }

        val key = "$slug:$email"
        val isNewKey = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", Duration.ofSeconds(emailProperties.sendCooldown))
            .awaitSingleOrNull()
            ?: false

        return isNewKey
    }
}
