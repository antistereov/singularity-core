package io.stereov.web.global.service.jwt.exception

class InvalidTokenException(message: String, cause: Throwable? = null) : TokenException(message, cause)
