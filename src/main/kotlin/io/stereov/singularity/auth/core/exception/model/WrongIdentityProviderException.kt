package io.stereov.singularity.auth.core.exception.model

import io.stereov.singularity.auth.core.exception.AuthenticationException

class WrongIdentityProviderException(msg: String, cause: Throwable? = null) : AuthenticationException(msg, cause) {
}