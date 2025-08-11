package io.stereov.singularity.auth.core.exception.model

import io.stereov.singularity.auth.core.exception.AuthException

/**
 * # Exception thrown when no token is provided.
 *
 * This exception is used to indicate that the request does not contain a token, which is required for authentication.
 * It extends the [AuthException] class.
 *
 * @property message The error message to be displayed.
 * @property cause The underlying cause of the exception, if any.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
class NoTokenProvidedException(message: String, cause: Throwable? = null) : AuthException(message, cause)
