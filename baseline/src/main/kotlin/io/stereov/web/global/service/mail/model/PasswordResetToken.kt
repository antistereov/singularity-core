package io.stereov.web.global.service.mail.model

data class PasswordResetToken(
    val userId: String,
    val secret: String,
)
