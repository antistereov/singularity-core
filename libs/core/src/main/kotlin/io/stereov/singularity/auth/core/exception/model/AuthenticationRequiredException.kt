package io.stereov.singularity.auth.core.exception.model

import io.stereov.singularity.auth.core.exception.AuthenticationException

class AuthenticationRequiredException(
    msg: String,
    cause: Throwable? = null
) : AuthenticationException(msg, CODE, cause) {

    companion object {
        const val CODE = "GROUP_MEMBERSHIP_REQUIRED"
    }
}