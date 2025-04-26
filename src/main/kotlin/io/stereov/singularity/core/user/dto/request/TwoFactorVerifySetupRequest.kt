package io.stereov.singularity.core.user.dto.request

data class TwoFactorVerifySetupRequest(
    val token: String,
    val code: Int,
)
