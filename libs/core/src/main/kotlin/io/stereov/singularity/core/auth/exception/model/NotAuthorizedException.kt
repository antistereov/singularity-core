package io.stereov.singularity.core.auth.exception.model

import io.stereov.singularity.core.auth.exception.AuthException

class NotAuthorizedException(msg: String, cause: Throwable? = null) : AuthException(msg, cause)
