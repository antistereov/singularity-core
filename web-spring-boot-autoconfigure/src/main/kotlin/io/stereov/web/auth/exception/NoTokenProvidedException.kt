package io.stereov.web.auth.exception

class NoTokenProvidedException(message: String, cause: Throwable? = null) : AuthException(message, cause)
