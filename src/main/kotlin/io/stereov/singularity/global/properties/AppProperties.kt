package io.stereov.singularity.global.properties

import io.stereov.singularity.auth.group.dto.request.CreateGroupRequest
import io.stereov.singularity.global.util.toSlug
import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.*

@ConfigurationProperties(prefix = "singularity.app")
data class AppProperties(
    // core
    val name: String = "Singularity",
    val baseUri: String = "http://localhost:8000",
    val supportEmail: String = "support@example.com",
    val secure: Boolean = false,
    private val defaultLocale: String = "en",

    // root user
    val createRootUser: Boolean = false,
    val rootEmail: String = "admin@example.com",
    val rootPassword: String = "strong-password",

    // groups
    val groups: List<CreateGroupRequest> = emptyList(),
) {

    val slug: String
        get() = name.toSlug()

    val locale: Locale = Locale.forLanguageTag(defaultLocale)
}
