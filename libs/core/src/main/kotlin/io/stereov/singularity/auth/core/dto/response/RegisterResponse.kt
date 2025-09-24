package io.stereov.singularity.auth.core.dto.response

import io.stereov.singularity.auth.geolocation.dto.GeolocationResponse
import io.stereov.singularity.user.core.dto.response.UserResponse

data class RegisterResponse(
    val user: UserResponse,
    var accessToken: String? = null,
    val refreshToken: String? = null,
    val location: GeolocationResponse? = null,
)
