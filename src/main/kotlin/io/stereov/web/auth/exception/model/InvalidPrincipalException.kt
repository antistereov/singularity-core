package io.stereov.web.auth.exception.model

import io.stereov.web.auth.exception.AuthException

/**
 * # Exception thrown when the principal is invalid.
 *
 * This exception is used to indicate that the provided principal is not valid or does not exist.
 * It extends the [AuthException] class.
 *
 * @property message The error message to be displayed.
 * @property cause The underlying cause of the exception, if any.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
class InvalidPrincipalException(message: String, cause: Throwable? = null) : AuthException(message, cause)
