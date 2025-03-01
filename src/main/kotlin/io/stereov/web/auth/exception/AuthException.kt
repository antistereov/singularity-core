package io.stereov.web.auth.exception

import io.stereov.web.global.exception.BaseWebException


open class AuthException(
    message: String,
    cause: Throwable? = null
) : BaseWebException(
    message,
    cause
)
