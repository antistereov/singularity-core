package io.stereov.web.user.dto

data class LoginRequest(
    val email: String,
    val password: String,
    val device: DeviceInfoRequest,
)
