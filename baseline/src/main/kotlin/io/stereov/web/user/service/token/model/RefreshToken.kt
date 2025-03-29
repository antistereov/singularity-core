package io.stereov.web.user.service.token.model

data class RefreshToken(
    val accountId: String,
    val deviceId: String,
    val value: String,
)
