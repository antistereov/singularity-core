package io.stereov.singularity.auth.twofactor.dto.response

data class TwoFactorRecoveryResponse(
    val accessToken: String?,
    val refreshToken: String?,
    val stepUpToken: String?
)