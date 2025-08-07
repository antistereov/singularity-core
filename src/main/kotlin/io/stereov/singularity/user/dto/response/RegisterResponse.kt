package io.stereov.singularity.user.dto.response

import io.stereov.singularity.user.dto.UserResponse

data class RegisterResponse(
    val user: UserResponse,
    var token: String? = null,
)