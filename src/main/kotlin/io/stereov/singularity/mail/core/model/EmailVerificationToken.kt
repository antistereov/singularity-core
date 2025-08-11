package io.stereov.singularity.mail.core.model

import org.bson.types.ObjectId

/**
 * # Email verification token.
 *
 * This data class represents an email verification token.
 * It contains the email address and the secret token.
 *
 * @property email The email address associated with the token.
 * @property secret The secret token for email verification.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class EmailVerificationToken(
    val userId: ObjectId,
    val email: String,
    val secret: String,
)
