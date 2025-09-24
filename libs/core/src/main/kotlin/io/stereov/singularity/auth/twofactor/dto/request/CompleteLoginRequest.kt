package io.stereov.singularity.auth.twofactor.dto.request

import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest

data class CompleteLoginRequest(
    override val email: String? = null,
    override val totp: Int? = null,
    val session: SessionInfoRequest? = null
) : TwoFactorAuthenticationRequest
