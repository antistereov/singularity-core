package io.stereov.singularity.auth.oauth2.model

data class CustomState(
    val randomState: String,
    val sessionTokenValue: String?,
    val redirectUri: String?,
    val oauth2ProviderConnectionTokenValue: String?,
    val stepUp: Boolean
)
