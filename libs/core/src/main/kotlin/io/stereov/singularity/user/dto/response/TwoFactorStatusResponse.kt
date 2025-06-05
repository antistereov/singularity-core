package io.stereov.singularity.user.dto.response

/**
 * # TwoFactorStatusResponse
 *
 * This data class represents the response for the two-factor authentication status.
 * It contains a single property `twoFactorRequired` which indicates
 * whether two-factor authentication is required or not.
 *
 * @property twoFactorRequired A boolean value indicating if two-factor authentication is required.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class TwoFactorStatusResponse(
    val twoFactorRequired: Boolean,
)
