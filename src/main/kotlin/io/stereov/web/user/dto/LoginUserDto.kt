package io.stereov.web.user.dto

data class LoginUserDto(
    val email: String,
    val password: String,
    val device: DeviceInfoRequestDto
)
