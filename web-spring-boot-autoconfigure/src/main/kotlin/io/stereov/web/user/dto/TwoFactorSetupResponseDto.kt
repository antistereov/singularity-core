package io.stereov.web.user.dto

import kotlinx.serialization.Serializable

@Serializable
data class TwoFactorSetupResponseDto(
    val secret: String,
    val optAuthUrl: String,
    val recoveryCode: String
)
