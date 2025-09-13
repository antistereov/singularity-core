package io.stereov.singularity.auth.oauth2.model

data class CustomState(
    val randomState: String,
    val sessionToken: String?,
    val redirectUri: String?
)
