package io.stereov.singularity.auth.core.dto.response

import com.maxmind.geoip2.model.CityResponse
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.user.core.dto.response.UserResponse

data class LoginResponse(
    val user: UserResponse,
    val accessToken: String?,
    val refreshToken: String?,
    val sessionToken: String?,
    val twoFactorRequired: Boolean,
    val allowedTwoFactorMethods: List<TwoFactorMethod>?,
    val twoFactorAuthenticationToken: String?,
    val location: CityResponse?
)
