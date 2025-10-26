package io.stereov.singularity.auth.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.model.IdentityProvider
import io.stereov.singularity.auth.oauth2.util.getWellKnownProvider
import io.stereov.singularity.email.core.service.EmailService
import io.stereov.singularity.email.core.util.EmailConstants
import io.stereov.singularity.email.template.service.TemplateService
import io.stereov.singularity.email.template.util.TemplateBuilder
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
    private val emailService: EmailService
) {

    private val logger = KotlinLogging.logger {}
    private val slug = "registration_alert"
    private val templatePath = "${EmailConstants.TEMPLATE_DIR}/$slug.html"

    suspend fun send(user: UserDocument, locale: Locale?) {
        logger.debug { "Sending registration alert email for user ${user.id}" }

        val actualLocale = locale ?: appProperties.locale

        val email = user.sensitive.email ?: return

        val subject = translateService.translateResourceKey(TranslateKey("$slug.subject"),
            EmailConstants.RESOURCE_BUNDLE, actualLocale)
        val content = TemplateBuilder.fromResource(templatePath)
            .translate(EmailConstants.RESOURCE_BUNDLE, actualLocale)
            .replacePlaceholders(templateService.getPlaceholders(mapOf(
                "name" to user.sensitive.name,
                "provider_placeholder" to getProvidersString(user, actualLocale)
            )))
            .build()

        emailService.sendEmail(email, subject, content, actualLocale)
    }


    private suspend fun getProvidersString(user: UserDocument, actualLocale: Locale): String {
        val providers = user.sensitive.identities.keys.map { getWellKnownProvider(it) }
        val passwordProviderString = translateService.translateResourceKey(TranslateKey("$slug.password_login"),
            EmailConstants.RESOURCE_BUNDLE, actualLocale)
        val oauth2ProviderStringWithPlaceholder = translateService.translateResourceKey(TranslateKey("$slug.oauth2_login"),
            EmailConstants.RESOURCE_BUNDLE, actualLocale)

        val list = providers.filter { it != IdentityProvider.PASSWORD }

        val or = translateService.translateResourceKey(TranslateKey("$slug.password_login"),
            EmailConstants.RESOURCE_BUNDLE, actualLocale)

        val oauth2Providers =  when (list.size) {
            0 -> ""
            1 -> list.first()
            else -> {
                val mutableList = list.toMutableList()
                val lastProvider = mutableList.removeLast()
                val otherProviders = mutableList.joinToString(", ")

                "$otherProviders $or $lastProvider"
            }
        }

        val oauth2ProviderString = oauth2ProviderStringWithPlaceholder
            .replace("{{ provider }}", oauth2Providers)

        return if (providers.contains(IdentityProvider.PASSWORD)) {
            if (list.isEmpty()) {
                passwordProviderString
            } else {
                "$passwordProviderString $or $oauth2ProviderString"
            }
        } else {
            if (list.isEmpty()) {
                "[ERROR: No identity provider found. Please contact the support.]"
            } else {
                oauth2ProviderString
            }
        }
    }
}
