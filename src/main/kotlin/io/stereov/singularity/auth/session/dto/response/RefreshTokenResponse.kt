package io.stereov.singularity.auth.session.dto.response

import io.stereov.singularity.user.core.dto.response.UserResponse

data class RefreshTokenResponse(
    val user: UserResponse,
    val accessToken: String? = null,
    val refreshToken: String? = null,
)
