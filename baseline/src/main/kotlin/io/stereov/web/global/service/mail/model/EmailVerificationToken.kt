package io.stereov.web.global.service.mail.model

data class EmailVerificationToken(
    val email: String,
    val secret: String,
)
