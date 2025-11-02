package io.stereov.singularity.auth.core.dto.request

import jakarta.validation.constraints.Email
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank

data class SendEmailVerificationRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,
)