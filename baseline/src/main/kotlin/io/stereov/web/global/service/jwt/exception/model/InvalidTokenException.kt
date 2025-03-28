package io.stereov.web.global.service.jwt.exception.model

import io.stereov.web.global.service.jwt.exception.TokenException

class InvalidTokenException(message: String, cause: Throwable? = null) : TokenException(message, cause)
