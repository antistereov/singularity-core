package io.stereov.web.user.service.token.model

/**
 * # SetupToken data class.
 *
 * This data class represents a token used for setting up two-factor authentication.
 * It contains the secret key and a recovery code.
 *
 * @property secret The secret key used for two-factor authentication setup.
 * @property recoveryCode The recovery code used for two-factor authentication setup.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class SetupToken(
    val secret: String,
    val recoveryCode: String,
)
