package io.stereov.singularity.email.template.service

import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.global.properties.UiProperties
import org.springframework.stereotype.Service

@Service
class TemplateService(
    appProperties: AppProperties,
    uiProperties: UiProperties
) {

    private val placeholders = mapOf(
        "app_uri" to uiProperties.baseUri.trim(),
        "app_name" to appProperties.name.trim(),
        "icon_uri" to uiProperties.iconUri.trim(),
        "contact_uri" to uiProperties.contactUri.trim(),
        "legal_notice_uri" to uiProperties.legalNoticeUri.trim(),
        "privacy_policy_uri" to uiProperties.privacyPolicyUri.trim(),
        "login_uri" to uiProperties.loginUri.trim(),
        "register_uri" to uiProperties.registerUri.trim(),
        "email_verification_uri" to uiProperties.emailVerificationUri.trim(),
        "password_reset_uri" to uiProperties.passwordResetUri.trim(),
        "security_settings_uri" to uiProperties.securitySettingsUri.trim(),
        "support_email" to appProperties.supportEmail.trim(),
        "primary_color" to uiProperties.primaryColor.trim(),
        "secondary_background_color" to uiProperties.secondaryBackgroundColor.trim(),
        "secondary_text_color" to uiProperties.secondaryTextColor.trim(),
    )

    fun getPlaceholders(additional: Map<String, Any>): Map<String, Any> {
        val customPlaceholders = mutableMapOf<String, Any>()
        customPlaceholders.putAll(placeholders)
        customPlaceholders.putAll(additional)

        return customPlaceholders
    }
}
