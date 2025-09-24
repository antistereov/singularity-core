package io.stereov.singularity.auth.guest.dto.request

import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest

data class CreateGuestRequest(
    val name: String,
    val session: SessionInfoRequest?
)
