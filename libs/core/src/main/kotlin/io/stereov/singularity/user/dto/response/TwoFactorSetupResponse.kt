package io.stereov.singularity.user.dto.response

/**
 * # Response object for two-factor authentication setup.
 *
 * This class contains the secret key, the OTP auth URL, and a recovery code.
 *
 * @property secret The secret key for the user.
 * @property optAuthUrl The OTP auth URL for the user.
 * @property recoveryCodes The recovery codes for the user.
 * @property token The JWT needed for verification.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class TwoFactorSetupResponse(
    val secret: String,
    val optAuthUrl: String,
    val recoveryCodes: List<String>,
    val token: String,
)
