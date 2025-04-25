package io.stereov.web.user.dto.request

data class TwoFactorVerifySetupRequest(
    val token: String,
    val code: Int,
)
