package io.stereov.singularity.user.session.dto.request

import jakarta.validation.constraints.NotBlank

/**
 * # Change password request.
 *
 * This data class represents a request to change a user's password.
 * It contains the old password, new password, and a two-factor authentication code.
 *
 * @property oldPassword The user's current password.
 * @property newPassword The new password to be set.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class ChangePasswordRequest(
    val oldPassword: String,
    @field:NotBlank(message = "New password required")
    val newPassword: String,
)
