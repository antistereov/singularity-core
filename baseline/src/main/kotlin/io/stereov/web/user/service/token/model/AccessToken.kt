package io.stereov.web.user.service.token.model

data class AccessToken(
    val userId: String,
    val deviceId: String,
)
