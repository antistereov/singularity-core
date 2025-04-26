package io.stereov.singularity.core.user.exception

/**
 * # UserException
 *
 * This class represents a custom exception for user-related errors.
 * It extends the RuntimeException class and provides constructors to set the error message and cause.
 *
 * @param message The error message.
 * @param cause The cause of the exception (optional).
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
open class UserException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
