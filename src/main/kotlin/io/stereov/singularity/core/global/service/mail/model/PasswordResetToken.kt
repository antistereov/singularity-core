package io.stereov.singularity.core.global.service.mail.model

/**
 * # Password reset token.
 *
 * This data class represents a password reset token.
 * It contains the user ID and the secret token.
 *
 * @property userId The ID of the user associated with the password reset token.
 * @property secret The secret token used for password reset.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class PasswordResetToken(
    val userId: String,
    val secret: String,
)
