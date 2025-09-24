package io.stereov.singularity.auth.oauth2.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.auth.oauth2")
data class OAuth2Properties(
    val enable: Boolean = false,
    val errorRedirectUri: String = "http://localhost:8000/auth/oauth2/error"
)
