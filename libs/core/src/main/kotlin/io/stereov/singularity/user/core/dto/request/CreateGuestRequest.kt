package io.stereov.singularity.user.core.dto.request

import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest

data class CreateGuestRequest(
    val name: String,
    val session: SessionInfoRequest?
)
