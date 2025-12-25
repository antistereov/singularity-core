package io.stereov.singularity.auth.twofactor.dto.response

import io.stereov.singularity.principal.core.dto.response.PrincipalResponse

data class TwoFactorRecoveryResponse(
    val user: PrincipalResponse,
    val accessToken: String?,
    val refreshToken: String?,
    val stepUpToken: String?,
)
