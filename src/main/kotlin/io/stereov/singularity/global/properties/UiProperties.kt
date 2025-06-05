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
    val iconPath: String = "/icon.svg",
    val acceptInvitationPath: String = "/invitation/accept",
    val contactPath: String = "/contact",
    val legalNoticePath: String = "/legal-notice",
    val privacyPolicyPath: String = "/privacy-policy"
)
