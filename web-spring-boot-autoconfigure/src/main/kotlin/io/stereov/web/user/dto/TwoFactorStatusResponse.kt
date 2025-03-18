package io.stereov.web.user.dto

import kotlinx.serialization.Serializable

@Serializable
data class TwoFactorStatusResponse(
    val twoFactorRequired: Boolean,
)
