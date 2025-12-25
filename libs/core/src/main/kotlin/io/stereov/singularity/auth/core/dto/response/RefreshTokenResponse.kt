package io.stereov.singularity.auth.core.dto.response

import io.stereov.singularity.principal.core.dto.response.PrincipalResponse

data class RefreshTokenResponse(
    val principal: PrincipalResponse,
    val accessToken: String? = null,
    val refreshToken: String? = null,
)
