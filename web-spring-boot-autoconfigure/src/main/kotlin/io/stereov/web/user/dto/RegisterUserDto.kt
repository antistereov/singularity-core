package io.stereov.web.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class RegisterUserDto(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,
    @field:NotBlank(message = "Password is required")
    val password: String,
    @field:NotBlank(message = "Name is required")
    val name: String,
    val device: DeviceInfoRequestDto,
    val guestId: String? = null,
)
