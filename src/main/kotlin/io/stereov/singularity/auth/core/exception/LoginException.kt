package io.stereov.singularity.auth.core.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.global.exception.SingularityException
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
sealed class LoginException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents a specific type of [AuthenticationException] indicating that the user is already authenticated.
     *
     * This exception is typically thrown when there is an attempt to authenticate an already authenticated user.
     *
     * @param msg The exception message providing details about the context of the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `ALREADY_AUTHENTICATED`
     * @property status [HttpStatus.NOT_MODIFIED]
     *
     * @see AlreadyAuthenticatedFailure
     * @see AuthenticationException.AlreadyAuthenticated
     */
    class AlreadyAuthenticated(msg: String, cause: Throwable? = null) : LoginException(
        msg,
        AlreadyAuthenticatedFailure.CODE,
        AlreadyAuthenticatedFailure.STATUS,
        AlreadyAuthenticatedFailure.DESCRIPTION,
        cause
    )

    /**
     * Represents a failure during the authentication process due to invalid credentials.
     *
     * This exception is thrown when a user provides incorrect or invalid authentication credentials,
     * such as a wrong username or password, during the login process.
     *
     * @param msg The exception message providing details about the context of the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @see InvalidCredentialsFailure
     */
    class InvalidCredentials(msg: String, cause: Throwable? = null) : LoginException(
        msg,
        InvalidCredentialsFailure.CODE,
        InvalidCredentialsFailure.STATUS,
        InvalidCredentialsFailure.DESCRIPTION,
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
    class Database(msg: String, cause: Throwable? = null) : LoginException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )
}
