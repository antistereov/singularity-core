package io.stereov.singularity.global.properties

import io.stereov.singularity.global.util.toSlug
import io.stereov.singularity.user.group.dto.CreateGroupRequest
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.app")
data class AppProperties(
    // core
    val name: String = "Singularity",
    val baseUrl: String = "http://localhost:8000",
    val supportMail: String = "support@example.com",
    val secure: Boolean = false,

    // root user
    val createRootUser: Boolean = false,
    val rootEmail: String = "admin@example.com",
    val rootPassword: String = "strong-password",

    // groups
    val groups: List<CreateGroupRequest> = emptyList(),

    // mail
    val enableMail: Boolean = false,
) {

    val slug: String
        get() = name.toSlug()
}
