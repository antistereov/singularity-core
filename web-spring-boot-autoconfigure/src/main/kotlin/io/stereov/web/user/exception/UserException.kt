package io.stereov.web.user.exception

open class UserException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
