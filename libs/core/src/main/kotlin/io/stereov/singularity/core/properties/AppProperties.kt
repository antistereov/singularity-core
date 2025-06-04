package io.stereov.singularity.core.properties

import io.stereov.singularity.core.group.dto.CreateGroupMultiLangRequest
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * # Application properties.
 *
 * This class is responsible for holding the application properties
 * and is annotated with [ConfigurationProperties]
 * to bind the properties from the application configuration file.
 *
 * It is prefixed with `baseline.app` in the configuration file.
 *
 * @property name The name of the application.
 * @property baseUrl The base URL of the application.
 * @property secure Indicates whether the application is secure (HTTPS).
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@ConfigurationProperties(prefix = "baseline.app")
data class AppProperties(
    val name: String = "Spring Boot Application",
    val baseUrl: String,
    val supportMail: String = "support@example.com",
    val secure: Boolean = false,
    val createRootUser: Boolean = false,
    val rootEmail: String = "admin@example.com",
    val rootPassword: String = "strong-password",
    val groups: List<CreateGroupMultiLangRequest> = emptyList()
) {

    val slug: String
        get() = name
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), "-")
}
