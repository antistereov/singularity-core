package io.stereov.singularity.user.dto.response

import io.stereov.singularity.user.dto.UserResponse

/**
 * # LoginResponse
 *
 * This data class represents the response returned after a user login attempt.
 * It contains information about whether two-factor authentication is required
 * and the user details.
 *
 * @property twoFactorRequired Indicates if two-factor authentication is required.
 * @property user The user details.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class LoginResponse(
    val twoFactorRequired: Boolean,
    val user: UserResponse,
)
