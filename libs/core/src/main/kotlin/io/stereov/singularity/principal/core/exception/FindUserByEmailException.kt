package io.stereov.singularity.principal.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions that occur when attempting to find a user by email.
 *
 * This sealed class extends [SingularityException], providing a structured way to handle various
 * error scenarios encountered during the process of locating a user by their email address.
 * Each specific subclass represents a particular type of failure, offering additional context
 * such as error codes, HTTP status, detailed descriptions, and optionally the underlying cause.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of error.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing additional context about the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class FindUserByEmailException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?,
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Exception thrown when a user with the specified email cannot be found.
     *
     * This exception is a specific subtype of [FindUserByEmailException] and provides
     * details about the failure to locate a user by their email address.
     *
     * @param msg A message providing context about the missing user.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `USER_NOT_FOUND`
     * @property status [HttpStatus.NOT_FOUND]
     */
    class NotFound(msg: String, cause: Throwable? = null) : FindUserByEmailException(
        msg,
        "USER_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "No user with specified email found.",
        cause
    )

    /**
     * Exception indicating a failure during hashing operations.
     *
     * This exception is a specific subtype of [FindUserByEmailException] and represents errors
     * related to the generation or verification of hashes, typically for user-related data.
     *
     * @param msg A message providing detailed context about the hashing failure.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `USER_HASH_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class HashFailure(msg: String, cause: Throwable? = null) : FindUserByEmailException(
        msg,
        "USER_HASH_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to generate or verify hash.",
        cause
    )

    /**
     * Represents an exception for database retrieval failures when attempting to find a user by email.
     *
     * This exception is a specific type of [FindUserByEmailException] and indicates that the user
     * could not be retrieved from the database due to a database-related issue. It provides
     * a consistent structure for error details such as error message, error code, HTTP status,
     * a detailed error description, and optionally, a cause.
     *
     * @param msg The error message describing the context of the failure.
     * @param cause The root cause of the exception, if available.
     *
     * @property code `USER_DB_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Database(msg: String, cause: Throwable? = null) : FindUserByEmailException(
        msg,
        "USER_DB_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to retrieve user from database.",
        cause
    )

    /**
     * Exception representing a user encryption or decryption failure.
     *
     * This exception is a specific type of `FindUserByEmailException`,
     * indicating that an error occurred during the encryption or decryption
     * process for user data. It encapsulates details about the failure, such
     * as an error message, a unique code, an HTTP status, a description of
     * the issue, and optionally the underlying cause.
     *
     * @param msg A message providing context for the encryption or decryption failure.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `USER_ENCRYPTION_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Encryption(msg: String, cause: Throwable? = null) : FindUserByEmailException(
        msg,
        "USER_ENCRYPTION_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to encrypt or decrypt user data.",
        cause
    )
}
