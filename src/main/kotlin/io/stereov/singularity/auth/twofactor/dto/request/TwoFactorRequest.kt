package io.stereov.singularity.auth.twofactor.dto.request

import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest

data class TwoFactorRequest(
    val mail: String?,
    val totp: Int?,
    val session: SessionInfoRequest
)
