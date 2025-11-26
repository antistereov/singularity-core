package io.stereov.singularity.principal.core.dto.response

import io.stereov.singularity.auth.geolocation.dto.GeolocationResponse

data class CreateGuestResponse(
    val user: UserResponse,
    val accessToken: String?,
    val refreshToken: String?,
    val location: GeolocationResponse?,
)
