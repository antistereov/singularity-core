package io.stereov.web.user.dto.request

data class TwoFactorSetupRequest(
    val token: String,
    val code: Int,
)
