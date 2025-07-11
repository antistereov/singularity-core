package io.stereov.singularity.secrets.bitwarden.properties

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.*

@ConfigurationProperties("singularity.secrets.bitwarden")
@ConditionalOnProperty(prefix = "singularity.secrets", value = ["store"], havingValue = "bitwarden", matchIfMissing = false)
data class BitwardenSecretStoreProperties(
    val apiUrl: String = "https://api.bitwarden.com",
    val identityUrl: String = "https://identity.bitwarden.com",
    val accessToken: String,
    val organizationId: UUID,
    val projectId: UUID,
    val stateFile: String,
)