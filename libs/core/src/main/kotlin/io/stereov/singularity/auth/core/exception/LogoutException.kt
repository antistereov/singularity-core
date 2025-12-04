package io.stereov.singularity.auth.core.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.global.exception.SingularityException
import io.stereov.singularity.principal.core.exception.UserNotFoundFailure
import org.springframework.http.HttpStatus

/**
 * Represents a sealed class for exceptions related to the login process.
 *
 * This class defines various specific exceptions that can occur during the authentication
 * and login process, such as already authenticated users, invalid credentials, or database errors.
 * It extends [SingularityException] to provide context including a message, error code, HTTP status,
 * description, and an optional cause.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of error.
 * @param status The HTTP status associated with the exception.
 * @param description A detailed description providing additional context about the exception.
 * @param cause The optional underlying cause of the exception.
 */
sealed class LogoutException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents a specific type of [LogoutException] indicating that the user has already logged out.
     *
     * This exception is typically thrown when an operation is attempted that assumes the user is logged in,
     * but the user has already logged out of the system.
     *
     * @param msg The exception message providing details about the context of the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `ALREADY_LOGGED_OUT`
     * @property status [HttpStatus.NOT_MODIFIED]
     */
    class AlreadyLoggedOut(msg: String, cause: Throwable? = null) : LogoutException(
        msg,
        "ALREADY_LOGGED_OUT",
        HttpStatus.NOT_MODIFIED,
        "User is already logged out.",
        cause
    )

    /**
     * Represents an exception indicating a failure during an encrypted database operation.
     *
     * This exception is thrown when a database operation encounters an error that prevents it
     * from being completed successfully. This may occur due to issues such as decryption failures,
     * corrupted data, or other internal database-related problems.
     *
     * @param msg A message providing details about the context of the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @see DatabaseFailure
     */
    class Database(msg: String, cause: Throwable? = null) : LogoutException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    /**
     * Represents a specific type of [LogoutException] indicating that a requested user could not be found.
     *
     * This exception is typically thrown when an operation attempts to interact with a user
     * that does not exist within the system's database. It provides additional context by including
     * a unique error code, an associated HTTP status, and a detailed description of the issue.
     *
     * @param msg The exception message providing details about the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @see UserNotFoundFailure
     */
    class NotFound(msg: String, cause: Throwable? = null) : LogoutException(
        msg,
        UserNotFoundFailure.CODE,
        UserNotFoundFailure.STATUS,
        UserNotFoundFailure.DESCRIPTION,
        cause
    )
}
