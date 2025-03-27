package io.stereov.web.user.dto

import kotlinx.serialization.Serializable

@Serializable
data class TwoFactorSetupResponse(
    val secret: String,
    val optAuthUrl: String,
    val recoveryCode: String
)
