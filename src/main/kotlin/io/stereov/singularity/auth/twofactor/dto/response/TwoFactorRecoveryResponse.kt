package io.stereov.singularity.auth.twofactor.dto.response

import io.stereov.singularity.user.core.dto.response.UserResponse

data class TwoFactorRecoveryResponse(
    val user: UserResponse,
    val accessToken: String?,
    val refreshToken: String?,
    val stepUpToken: String?
)
