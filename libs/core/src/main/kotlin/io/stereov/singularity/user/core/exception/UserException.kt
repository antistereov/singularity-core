package io.stereov.singularity.user.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions related to user operations.
 *
 * This sealed class serves as a base for specific exceptions that may occur
 * when handling user-related operations. It extends [SingularityException]
 * to provide a consistent structure for error details, including a message,
 * error code, HTTP status, detailed description, and optionally a root cause.
 *
 * @param msg The error message providing context for the exception.
 * @param code A unique code identifying the specific type of user-related error.
 * @param status The corresponding HTTP status that represents this error.
 * @param description A detailed description of the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class UserException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Exception representing a missing password authentication configuration for a user.
     *
     * This exception is thrown when attempting to access the password of a user who has
     * not configured password-based authentication.
     * It is a specific type of [UserException].
     *
     * @param msg A message describing the details of the error.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `NO_PASSWORD_PROVIDER`
     * @property status [HttpStatus.BAD_REQUEST]
     */
    class NoPassword(msg: String, cause: Throwable? = null) : UserException(
        msg,
        "NO_PASSWORD_PROVIDER",
        HttpStatus.BAD_REQUEST,
        "Thrown when trying to access a password of a user that did not configure password authentication.",
        cause,
    )

    /**
     * Exception representing an attempt to access a two-factor authentication secret
     * for a user who has not enabled two-factor authentication.
     *
     * This exception is a specific type of [UserException], indicating that the user
     * has not configured two-factor authentication and therefore the requested action
     * cannot be performed.
     *
     * @param msg A message providing details about the error.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `TWO_FACTOR_DISABLED`
     * @property status [HttpStatus.BAD_REQUEST]
     */
    class TwoFactorDisabled(msg: String, cause: Throwable? = null) : UserException(
        msg,
        "TWO_FACTOR_DISABLED",
        HttpStatus.BAD_REQUEST,
        "Thrown when trying to access a two-factor secret of a user that has not enabled two-factor authentication.",
        cause
    )
}
