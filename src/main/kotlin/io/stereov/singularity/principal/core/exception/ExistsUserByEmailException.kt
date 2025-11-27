package io.stereov.singularity.principal.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions related to operations involving user existence by email.
 *
 * This sealed class serves as a base for specific exceptions that may occur
 * when handling operations related to checking the existence of a user by their email.
 * It extends [SingularityException] to provide a consistent structure for error details,
 * including a message, error code, HTTP status, detailed description, and optionally a root cause.
 *
 * @param msg The error message providing context for the exception.
 * @param code A unique code identifying the specific type of email-related error.
 * @param status The corresponding HTTP status that represents this error.
 * @param description A detailed description of the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class ExistsUserByEmailException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?,
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Exception representing a failure in generating or verifying a hash.
     *
     * This exception is thrown when an operation fails due to issues with
     * hash generation or verification. It extends the [ExistsUserByEmailException]
     * to provide a consistent structure for errors related to user email operations.
     *
     * @param msg A message describing the details of the error.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `USER_HASH_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Hash(msg: String, cause: Throwable? = null) : ExistsUserByEmailException(
        msg,
        "USER_HASH_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to generate or verify hash.",
        cause
    )

    /**
     * Exception representing a failure to retrieve a user from the database.
     *
     * This exception is thrown when an operation involving user data retrieval
     * from the database encounters an error. It extends the [ExistsUserByEmailException]
     * to provide a structured error representation specific to database-related
     * user retrieval failures.
     *
     * @param msg A message describing the details of the error.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `USER_DB_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Database(msg: String, cause: Throwable? = null) : ExistsUserByEmailException(
        msg,
        "USER_DB_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to retrieve user from database.",
        cause
    )
}
