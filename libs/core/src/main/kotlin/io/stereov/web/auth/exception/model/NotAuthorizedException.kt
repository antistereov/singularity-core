package io.stereov.web.auth.exception.model

import io.stereov.web.auth.exception.AuthException

class NotAuthorizedException(msg: String, cause: Throwable? = null) : AuthException(msg, cause)
