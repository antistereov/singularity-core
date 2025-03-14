package io.stereov.web.user.dto

data class LoginResponse(
    val twoFactorRequired: Boolean,
    val user: UserDto?,
)
