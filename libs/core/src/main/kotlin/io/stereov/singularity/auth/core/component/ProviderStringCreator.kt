package io.stereov.singularity.auth.core.component

import io.stereov.singularity.auth.core.model.IdentityProvider
import io.stereov.singularity.auth.oauth2.util.getWellKnownProvider
import io.stereov.singularity.email.core.util.EmailConstants
import io.stereov.singularity.translate.model.TranslateKey
import io.stereov.singularity.translate.service.TranslateService
import io.stereov.singularity.user.core.model.UserDocument
import org.springframework.stereotype.Component
import java.util.*

@Component
class ProviderStringCreator(
    private val translateService: TranslateService,
) {

    suspend fun getProvidersString(user: UserDocument, actualLocale: Locale): String {
        val providers = user.sensitive.identities.keys.map { getWellKnownProvider(it) }
        val passwordProviderString = translateService.translateResourceKey(
            TranslateKey("password_login"),
            EmailConstants.RESOURCE_BUNDLE, actualLocale
        )
        val oauth2ProviderStringWithPlaceholder = translateService.translateResourceKey(
            TranslateKey("oauth2_login"),
            EmailConstants.RESOURCE_BUNDLE, actualLocale
        )

        val list = providers.filter { it != IdentityProvider.PASSWORD }

        val or = translateService.translateResourceKey(
            TranslateKey("or"),
            EmailConstants.RESOURCE_BUNDLE, actualLocale
        )

        val oauth2Providers = when (list.size) {
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