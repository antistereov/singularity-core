package io.stereov.singularity.user.dto.request

data class TwoFactorVerifySetupRequest(
    val token: String,
    val code: Int,
)
