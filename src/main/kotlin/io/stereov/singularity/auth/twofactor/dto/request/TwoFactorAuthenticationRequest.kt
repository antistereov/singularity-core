package io.stereov.singularity.auth.twofactor.dto.request

import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest

data class TwoFactorAuthenticationRequest(
    val mail: String? = null,
    val totp: Int? = null,
    val session: SessionInfoRequest? = null
)
