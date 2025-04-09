package io.stereov.web.user.dto.response

import kotlinx.serialization.Serializable

/**
 * # Response object for two-factor authentication setup.
 *
 * This class contains the secret key, the OTP auth URL, and a recovery code.
 *
 * @property secret The secret key for the user.
 * @property optAuthUrl The OTP auth URL for the user.
 * @property recoveryCode The recovery code for the user.
 * @property token The JWT needed for verification.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Serializable
data class TwoFactorSetupResponse(
    val secret: String,
    val optAuthUrl: String,
    val recoveryCode: String,
    val token: String,
)
