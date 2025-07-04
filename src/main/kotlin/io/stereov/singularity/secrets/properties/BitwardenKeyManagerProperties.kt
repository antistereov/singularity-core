package io.stereov.singularity.secrets.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.*

@ConfigurationProperties("singularity.secrets.bitwarden")
data class BitwardenKeyManagerProperties(
    val apiUrl: String = "https://api.bitwarden.com",
    val identityUrl: String = "https://identity.bitwarden.com",
    val accessToken: String,
    val organizationId: UUID,
    val projectId: UUID,
    val stateFile: String,
)
