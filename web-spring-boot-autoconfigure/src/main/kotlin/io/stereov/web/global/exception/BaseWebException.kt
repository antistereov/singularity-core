package io.stereov.web.global.exception

open class BaseWebException(
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
