package io.stereov.singularity.core.global.service.jwt.exception.model

import io.stereov.singularity.core.global.service.jwt.exception.TokenException

/**
 * # Invalid token exception.
 *
 * This exception is thrown when a token is invalid or cannot be verified.
 *
 * @param message The error message.
 * @param cause The cause of the exception, if any.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
class InvalidTokenException(message: String, cause: Throwable? = null) : TokenException(message, cause)
