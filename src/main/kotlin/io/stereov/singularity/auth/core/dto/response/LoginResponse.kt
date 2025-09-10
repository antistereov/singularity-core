package io.stereov.singularity.auth.core.dto.response

import com.maxmind.geoip2.model.CityResponse
import io.stereov.singularity.user.core.dto.response.UserResponse

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
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val twoFactorLoginToken: String? = null,
    val location: CityResponse? = null,
)
