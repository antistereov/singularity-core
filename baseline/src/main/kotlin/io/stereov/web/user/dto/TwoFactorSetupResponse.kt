package io.stereov.web.user.dto

import kotlinx.serialization.Serializable

/**
 * # Response object for two-factor authentication setup.
 *
 * This class contains the secret key, the OTP auth URL, and a recovery code.
 *
 * @property secret The secret key for the user.
 * @property optAuthUrl The OTP auth URL for the user.
 * @property recoveryCode The recovery code for the user.
 */
@Serializable
data class TwoFactorSetupResponse(
    val secret: String,
    val optAuthUrl: String,
    val recoveryCode: String
)
