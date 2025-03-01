package io.stereov.web.global.service.jwt.exception

import io.stereov.web.global.exception.BaseWebException

open class TokenException(message: String, cause: Throwable? = null) : BaseWebException(message, cause)
