package io.stereov.singularity.secrets.infisical.properties

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("singularity.secrets.infisical")
@ConditionalOnProperty(prefix = "singularity.secrets", value = ["store"], havingValue = "infisical", matchIfMissing = false)
data class InfisicalSecretStoreProperties(
    val url: String = "https://app.infisical.com",
    val clientId: String,
    val clientSecret: String,
    val projectId: String,
    val environmentSlug: String,
    val secretPath: String = "/"
)
