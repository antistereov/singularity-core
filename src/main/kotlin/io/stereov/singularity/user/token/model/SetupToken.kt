package io.stereov.singularity.user.token.model

/**
 * # SetupToken data class.
 *
 * This data class represents a token used for setting up two-factor authentication.
 * It contains the secret key and a recovery code.
 *
 * @property secret The secret key used for two-factor authentication setup.
 * @property recoveryCodes The recovery codes used for recovering the account when access to 2FA codes is lost.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class SetupToken(
    val secret: String,
    val recoveryCodes: List<String>,
)
