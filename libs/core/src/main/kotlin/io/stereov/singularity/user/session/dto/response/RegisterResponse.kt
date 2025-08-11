package io.stereov.singularity.user.session.dto.response

import io.stereov.singularity.user.core.dto.response.UserResponse

data class RegisterResponse(
    val user: UserResponse,
    var token: String? = null,
)
