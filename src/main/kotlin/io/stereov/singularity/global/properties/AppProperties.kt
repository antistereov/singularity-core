package io.stereov.singularity.global.properties

import io.stereov.singularity.global.util.toSlug
import io.stereov.singularity.user.group.dto.CreateGroupRequest
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.app")
data class AppProperties(
    val name: String = "Spring Boot Application",
    val baseUrl: String = "http://localhost:8000",
    val supportMail: String = "support@example.com",
    val secure: Boolean = false,
    val createRootUser: Boolean = false,
    val rootEmail: String = "admin@example.com",
    val rootPassword: String = "strong-password",
    val groups: List<CreateGroupRequest> = emptyList(),
    val enableMail: Boolean = false,
) {

    val slug: String
        get() = name.toSlug()
}
