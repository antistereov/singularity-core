package io.stereov.singularity.global.properties

import io.stereov.singularity.group.dto.CreateGroupMultiLangRequest
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.app")
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
