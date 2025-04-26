package io.stereov.singularity.core.global.exception

/**
 * # Base class for all web-related exceptions.
 *
 * This class extends the [RuntimeException] and is used to represent various web-related errors.
 *
 * @property message The error message to be displayed.
 * @property cause The underlying cause of the exception, if any.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
open class BaseWebException(
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
