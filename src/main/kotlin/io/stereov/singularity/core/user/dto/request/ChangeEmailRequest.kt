package io.stereov.singularity.core.user.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

/**
 * # ChangeEmailRequest data class.
 *
 * This data class represents a request to change a user's email address.
 * It contains the new email address, the user's password, and a two-factor authentication code.
 *
 * @property newEmail The new email address to be set for the user.
 * @property password The user's password for authentication.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class ChangeEmailRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val newEmail: String,
    val password: String,
)
