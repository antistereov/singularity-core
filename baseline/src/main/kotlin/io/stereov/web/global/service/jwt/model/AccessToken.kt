package io.stereov.web.global.service.jwt.model

data class AccessToken(
    val userId: String,
    val deviceId: String,
)
