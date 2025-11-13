package io.stereov.singularity.auth.core.exception.model

import io.stereov.singularity.auth.core.exception.AuthenticationException

class InvalidCredentialsException(
    msg: String = "Authentication failed: Invalid credentials"
) : AuthenticationException(msg, CODE) {

    companion object {
        const val CODE = "INVALID_CREDENTIALS"
    }
}
