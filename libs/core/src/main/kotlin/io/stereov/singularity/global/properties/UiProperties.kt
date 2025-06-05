package io.stereov.singularity.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * # UI properties.
 *
 * This class is responsible for holding the UI properties
 * and is annotated with [ConfigurationProperties]
 * to bind the properties from the application configuration file.
 *
 * It is prefixed with `baseline.ui` in the configuration file.
 *
 * @property baseUrl The base URL of the UI application.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@ConfigurationProperties(prefix = "singularity.ui")
data class UiProperties(
    val baseUrl: String = "http://localhost:4200",
    val iconUrl: String = "http://localhost:4200/icon.svg",
    val primaryColor: String = "#6366f1",
    val contactPath: String = "/contact",
    val legalNoticePath: String = "/legal-notice",
    val privacyPolicyPath: String = "/privacy-policy",
    val emailVerificationPath: String = "/auth/verify-email",
    val passwordResetPath: String = "/auth/reset-password",
)
