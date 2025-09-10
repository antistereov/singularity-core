package io.stereov.singularity.auth.core.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class SendPasswordResetRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,
)
