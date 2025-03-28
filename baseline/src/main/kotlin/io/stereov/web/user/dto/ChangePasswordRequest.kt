package io.stereov.web.user.dto

import jakarta.validation.constraints.NotBlank

data class ChangePasswordRequest(
    val oldPassword: String,
    @field:NotBlank(message = "New password required")
    val newPassword: String,
    val twoFactorCode: Int,
)
