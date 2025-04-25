package io.stereov.web.global.service.jwt.exception.model

import io.stereov.web.global.service.jwt.exception.TokenException

/**
 * # Exception thrown when a token has expired.
 *
 * This exception is thrown when a token has expired and is no longer valid.
 *
 * @param message The message to be displayed when the exception is thrown.
 * @param cause The cause of the exception, if any.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
class TokenExpiredException(message: String, cause: Throwable? = null) : TokenException(message, cause)
