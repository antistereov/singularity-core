package io.stereov.singularity.auth.oauth2.dto.request

import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest

data class OAuth2ProviderConnectionRequest(
    val provider: String,
    val session: SessionInfoRequest
)
