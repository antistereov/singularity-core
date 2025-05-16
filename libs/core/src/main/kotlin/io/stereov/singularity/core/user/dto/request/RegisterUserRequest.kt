package io.stereov.singularity.core.user.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
/**
 * # RegisterUserRequest data class.
 *
 * This data class represents a request to register a new user.
 * It contains the user's email, password, optional name, and device information.
 *
 * @property email The email address of the user (required).
 * @property password The password for the user account (required).
 * @property name The name of the user (optional).
 * @property device The device information associated with the user (required).
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class RegisterUserRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,
    @field:NotBlank(message = "Password is required")
    val password: String,
    @field:NotBlank
    val name: String,
    val device: DeviceInfoRequest,
)
