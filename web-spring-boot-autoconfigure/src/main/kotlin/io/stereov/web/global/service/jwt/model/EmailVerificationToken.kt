package io.stereov.web.global.service.jwt.model

data class EmailVerificationToken(
    val email: String,
    val uuid: String,
)
