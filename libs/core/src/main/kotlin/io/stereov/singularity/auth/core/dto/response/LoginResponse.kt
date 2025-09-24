package io.stereov.singularity.auth.core.dto.response

import io.stereov.singularity.auth.geolocation.dto.GeolocationResponse
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.user.core.dto.response.UserResponse

data class LoginResponse(
    val user: UserResponse,
    val accessToken: String?,
    val refreshToken: String?,
    val twoFactorRequired: Boolean,
    val allowedTwoFactorMethods: List<TwoFactorMethod>?,
    val preferredTwoFactorMethod: TwoFactorMethod?,
    val twoFactorAuthenticationToken: String?,
    val location: GeolocationResponse?
)
