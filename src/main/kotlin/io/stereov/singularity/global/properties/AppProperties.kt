package io.stereov.singularity.global.properties

import com.github.slugify.Slugify
import io.stereov.singularity.group.dto.CreateGroupRequest
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
    val groups: List<CreateGroupRequest> = emptyList()
) {

    val slug: String
        get() = Slugify.builder().build().slugify(name)
}
