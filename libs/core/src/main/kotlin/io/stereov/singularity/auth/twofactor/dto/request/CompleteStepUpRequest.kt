package io.stereov.singularity.auth.twofactor.dto.request

data class CompleteStepUpRequest(
    override val email: String? = null,
    override val totp: Int? = null,
) : TwoFactorAuthenticationRequest
