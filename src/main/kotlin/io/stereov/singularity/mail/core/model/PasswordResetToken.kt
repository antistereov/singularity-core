package io.stereov.singularity.mail.core.model

import io.stereov.singularity.auth.core.model.Token
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.Jwt

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
    val userId: ObjectId,
    val secret: String,
    override val jwt: Jwt
) : Token
