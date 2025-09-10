package io.stereov.singularity.auth.core.dto.request

/**
 * # ResetPasswordRequest data class.
 *
 * This data class represents a request to reset a user's password.
 * It contains the new password to be set for the user.
 *
 * @property newPassword The new password to be set for the user.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class ResetPasswordRequest(
    val newPassword: String
)
