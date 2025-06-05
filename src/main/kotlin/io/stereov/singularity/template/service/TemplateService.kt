package io.stereov.singularity.template.service

import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.global.properties.UiProperties
import org.springframework.stereotype.Service

@Service
class TemplateService(
    appProperties: AppProperties,
    uiProperties: UiProperties
) {

    private val placeholders = mapOf(
        "app_url" to uiProperties.baseUrl.trim(),
        "app_name" to appProperties.name.trim(),
        "icon_url" to uiProperties.baseUrl.removeSuffix("/").trim() + "/" + uiProperties.iconPath.removePrefix("/").trim(),
        "contact_url" to uiProperties.baseUrl.removeSuffix("/").trim() + "/" + uiProperties.contactPath.removePrefix("/").trim(),
        "legal_notice_url" to uiProperties.baseUrl.removeSuffix("/").trim() + "/" + uiProperties.legalNoticePath.removePrefix("/").trim(),
        "privacy_policy_url" to uiProperties.baseUrl.removeSuffix("/").trim() + "/" + uiProperties.privacyPolicyPath.removePrefix("/").trim(),
        "support_mail" to appProperties.supportMail.trim()
    )

    fun getPlaceholders(additional: Map<String, Any>): Map<String, Any> {
        val customPlaceholders = mutableMapOf<String, Any>()
        customPlaceholders.putAll(placeholders)
        customPlaceholders.putAll(additional)

        return customPlaceholders
    }
}
