package io.stereov.singularity.global.exception

/**
 * Base class for all web-related exceptions.
 *
 * This class extends the [RuntimeException] and is used to represent various web-related errors.
 *
 * @property msg The error message to be displayed.
 * @property cause The underlying cause of the exception, if any.
 * @property code The error code.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
open class SingularityException(
    msg: String,
    val code: String,
    cause: Throwable? = null,
) : RuntimeException(msg, cause)
