package io.stereov.singularity.auth.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.model.NoAccountInfoAction
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.email.core.service.EmailService
import io.stereov.singularity.email.core.util.EmailConstants
import io.stereov.singularity.email.template.service.TemplateService
import io.stereov.singularity.email.template.util.TemplateBuilder
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.translate.model.TranslateKey
import io.stereov.singularity.translate.service.TranslateService
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.*

@Service
class NoAccountInfoService(
    private val appProperties: AppProperties,
    private val translateService: TranslateService,
    private val emailService: EmailService,
    private val templateService: TemplateService,
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val emailProperties: EmailProperties
) {

    private val logger = KotlinLogging.logger {  }
    private val slug = "no_account_info"

    suspend fun send(
        email: String,
        action: NoAccountInfoAction,
        locale: Locale?,
    ) {
        logger.debug { "Sending no account info email to ${email}; reason: $action" }

        if (cooldownActive(email)) {
            logger.debug { "Skipping sending of email because cooldown is still active" }
            return
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
            TranslateKey("$slug.message.${action.value}"),
            EmailConstants.RESOURCE_BUNDLE, actualLocale)

        val content = TemplateBuilder.fromResource(templatePath)
            .translate(EmailConstants.RESOURCE_BUNDLE, actualLocale)
            .replacePlaceholders(templateService.getPlaceholders(mapOf(
                "action" to action,
            )))
            .build()

        emailService.sendEmail(email, subject, content, actualLocale)
        startCooldown(email)
    }

    private suspend fun cooldownActive(email: String): Boolean {
        logger.debug { "Getting remaining cooldown for email verification" }

        val key = "$slug:$email"
        val remainingTtl = redisTemplate.getExpire(key).awaitSingleOrNull() ?: Duration.ofSeconds(-1)

        return (remainingTtl.seconds > 0)
    }

    private suspend fun startCooldown(email: String): Boolean {
        logger.debug { "Starting cooldown for email verification" }

        val key = "$slug:$email"
        val isNewKey = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", Duration.ofSeconds(emailProperties.sendCooldown))
            .awaitSingleOrNull()
            ?: false

        return isNewKey
    }
}
