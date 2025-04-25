package io.stereov.web.auth.exception

import io.stereov.web.global.exception.BaseWebException

/**
 * # Base class for all authentication-related exceptions.
 *
 * This class extends the [BaseWebException] and is used to represent various authentication errors.
 *
 * @property message The error message to be displayed.
 * @property cause The underlying cause of the exception, if any.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
open class AuthException(
    message: String,
    cause: Throwable? = null
) : BaseWebException(
    message,
    cause
)
