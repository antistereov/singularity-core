package io.stereov.web.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class ChangeEmailRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val newEmail: String,
    val password: String,
    val twoFactorCode: Int,
)
