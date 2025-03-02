package io.stereov.web.user.dto

import io.stereov.web.user.model.Role
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val username: String?,
    val name: String?,
    val email: String,
    val roles: List<Role> = listOf(Role.USER),
    val emailVerified: Boolean = false,
    val devices: List<DeviceInfoResponseDto> = listOf(),
    val lastActive: String,
)
