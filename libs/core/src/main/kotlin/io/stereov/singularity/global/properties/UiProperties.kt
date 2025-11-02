package io.stereov.singularity.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.ui")
data class UiProperties(
    val primaryColor: String = "#6366f1",
    val secondaryBackgroundColor: String = "#E5E5E5",
    val secondaryTextColor: String = "#404040",

    val baseUri: String = "http://localhost:4200",

    val iconUri: String = "${baseUri}/icon.svg",

    val contactUri: String = "${baseUri}/contact",
    val legalNoticeUri: String = "${baseUri}/legal-notice",
    val privacyPolicyUri: String = "${baseUri}/privacy-policy",

    val loginUri: String = "${baseUri}/auth",
    val registerUri: String = "${baseUri}/auth/register",
    val emailVerificationUri: String = "${baseUri}/auth/verify-email",
    val passwordResetUri: String = "${baseUri}/auth/reset-password",

    val securitySettingsUri: String = "${baseUri}/me"
)
