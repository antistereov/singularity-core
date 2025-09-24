package io.stereov.singularity.auth.twofactor.dto.request

interface TwoFactorAuthenticationRequest {
    val totp: Int?
    val email: String?
}