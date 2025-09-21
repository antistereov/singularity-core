package io.stereov.singularity.auth.guest.dto.response

import io.stereov.singularity.auth.geolocation.dto.GeolocationResponse
import io.stereov.singularity.user.core.dto.response.UserResponse

data class ConvertToUserResponse(
    val user: UserResponse,
    val accessToken: String?,
    val refreshToken: String?,
    val location: GeolocationResponse?,
)