package io.stereov.singularity.auth.core.dto.response

import com.maxmind.geoip2.model.CityResponse
import io.stereov.singularity.user.core.dto.response.UserResponse

data class RegisterResponse(
    val user: UserResponse,
    var accessToken: String? = null,
    val refreshToken: String? = null,
    val sessionToken: String? = null,
    val location: CityResponse? = null,
)
