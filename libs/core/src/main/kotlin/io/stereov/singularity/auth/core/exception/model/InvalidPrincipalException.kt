package io.stereov.singularity.auth.core.exception.model

import io.stereov.singularity.auth.core.exception.AuthenticationException

class InvalidPrincipalException(
    message: String, cause: Throwable? = null
) : AuthenticationException(message, CODE, cause) {

    companion object {
        const val CODE = "INVALID_PRINCIPAL"
    }
}
