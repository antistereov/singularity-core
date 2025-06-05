package io.stereov.singularity.auth.exception.model

import io.stereov.singularity.auth.exception.AuthException

class NotAuthorizedException(msg: String, cause: Throwable? = null) : AuthException(msg, cause)
