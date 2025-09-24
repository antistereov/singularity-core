package io.stereov.singularity.auth.core.dto.request

data class LoginRequest(
    val email: String,
    val password: String,
    val session: SessionInfoRequest? = null,
)
