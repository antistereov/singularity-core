package io.stereov.web.global.service.jwt.exception

class TokenExpiredException(message: String, cause: Throwable? = null) : TokenException(message, cause)
