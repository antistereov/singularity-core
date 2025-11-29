package io.stereov.singularity.auth.core.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.hash.exception.HashFailure
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents a base exception class for handling errors specific to step-up authentication scenarios.
 *
 * Extends [SingularityException] and provides additional context such as error codes,
 * HTTP status, and descriptions for errors related to step-up authentication processes.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of error.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing more context about the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class StepUpException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents a specific type of exception indicating that a user's session has expired.
     *
     * This exception is typically thrown when an action or request is attempted by a user
     * whose session is no longer valid. It provides additional context including an error code,
     * associated HTTP status, and a description of the issue.
     *
     * @param msg The exception message providing details about the context of the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `SESSION_EXPIRED`
     * @property status [HttpStatus.UNAUTHORIZED]
     */
    class Session(msg: String, cause: Throwable? = null) : StepUpException(
        msg,
        "SESSION_EXPIRED",
        HttpStatus.UNAUTHORIZED,
        "Indicates that the user's session has expired.",
        cause
    )

    /**
     * Exception indicating that the user does not have a password set.
     *
     * This exception is thrown during operations where a password is required,
     * but the user has no password configured. This can occur in cases where
     * password-based authentication is attempted for a user that was created
     * or exists without a password.
     *
     * @param msg The exception message providing details about the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `NO_PASSWORD`
     * @property status [HttpStatus.BAD_REQUEST]
     */
    class NoPassword(msg: String, cause: Throwable? = null) : StepUpException(
        msg,
        "NO_PASSWORD",
        HttpStatus.BAD_REQUEST,
        "Indicates that the user does not have a password set.",
        cause
    )

    /**
     * Represents a specific type of [StepUpException] indicating that a required password parameter is missing.
     *
     * This exception is typically thrown during a request validation process when a password parameter
     * that is required for the operation is not present in the request. It provides additional context
     * including an error code, HTTP status, and a detailed description of the issue.
     *
     * @param msg The exception message providing details about the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `MISSING_PASSWORD_PARAMETER`
     * @property status [HttpStatus.BAD_REQUEST]
     * @property description `Indicates that the request is missing a password parameter.`
     */
    class MissingPasswordParameter(msg: String, cause: Throwable? = null) : StepUpException(
        msg,
        "MISSING_PASSWORD_PARAMETER",
        HttpStatus.BAD_REQUEST,
        "Indicates that the request is missing a password parameter.",
        cause
    )

    /**
     * Represents an exception specific to password hashing failures.
     *
     * This exception is thrown when an error occurs during the hashing process,
     * which is typically used to securely store passwords. It provides context such
     * as an error message, unique error code, associated HTTP status, a detailed description
     * of the error, and optionally the underlying cause of the failure.
     *
     * Extends [StepUpException].
     *
     * @param msg The exception message describing the hashing error.
     * @param cause The optional underlying cause of the exception.
     *
     * @see HashFailure
     *
     * @see io.stereov.singularity.database.hash.exception.HashException.Hashing
     */
    class Hashing(msg: String, cause: Throwable? = null) : StepUpException(
        msg,
        HashFailure.CODE,
        HashFailure.STATUS,
        HashFailure.DESCRIPTION,
        cause
    )


    /**
     * Represents an exception indicating a failure in the authentication process due to invalid credentials.
     *
     * This exception is thrown when a user provides incorrect or invalid authentication credentials,
     * such as an incorrect username or password, during the login process.
     *
     * @param msg The exception message providing details about the context of the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @see InvalidCredentialsFailure
     */
    class InvalidCredentials(msg: String, cause: Throwable? = null) : StepUpException(
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
    class Database(msg: String, cause: Throwable? = null) : StepUpException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )
}
