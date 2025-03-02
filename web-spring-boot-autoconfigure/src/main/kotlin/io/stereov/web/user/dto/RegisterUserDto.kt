package io.stereov.web.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class RegisterUserDto(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,
    @field:NotBlank(message = "Password is required")
    val password: String,
    val username: String? = null,
    val name: String? = null,
    val device: DeviceInfoRequestDto,
)
