package io.stereov.singularity.auth.twofactor.dto.request

import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest

data class TotpRecoveryRequest(
    val code: String,
    val session: SessionInfoRequest?
)
