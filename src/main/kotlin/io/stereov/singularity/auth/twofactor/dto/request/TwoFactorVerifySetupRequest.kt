package io.stereov.singularity.auth.twofactor.dto.request

data class TwoFactorVerifySetupRequest(
    val token: String,
    val code: Int,
)
