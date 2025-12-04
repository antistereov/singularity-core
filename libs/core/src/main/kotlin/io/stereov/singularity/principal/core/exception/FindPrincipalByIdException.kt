package io.stereov.singularity.principal.core.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.encryption.exception.DatabaseEncryptionFailure
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions specifically related to the failure of locating a principal by its identifier.
 *
 * This sealed class serves as a base for more specific exceptions that may arise when
 * attempting to find or handle a principal by its unique identifier, such as email or ID.
 * It extends [SingularityException] to provide a consistent structure for error handling,
 * including message, error code, HTTP status, detailed description, and optional underlying cause.
 *
 * @param msg The error message describing the issue in detail.
 * @param code A unique code representing the type of error.
 * @param status The corresponding HTTP status associated with this exception.
 * @param description A detailed description providing contextual information about the error.
 * @param cause The underlying cause of the exception, if any.
 */
sealed class FindPrincipalByIdException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?,
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Exception thrown when a principal cannot be located.
     *
     * This exception is a specific type of [FindPrincipalByIdException] that is used
     * to indicate that no principal with the specified email could be found in the system.
     * It provides consistent error details, including a message, error code, HTTP status,
     * and a default description for the error.
     *
     * @param msg A message providing additional details about the failure.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `PRINCIPAL_NOT_FOUND`
     * @property status [HttpStatus.NOT_FOUND]
     */
    class NotFound(msg: String, cause: Throwable? = null) : FindPrincipalByIdException(
        msg,
        "PRINCIPAL_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "No principal with specified ID found.",
        cause
    )

    /**
     * Exception indicating a failure to retrieve a principal from the database.
     *
     * This exception is a specific type of [FindPrincipalByIdException] used to signal
     * backend database issues when attempting to locate or fetch a principal. It contains
     * consistent error details, such as an error code, HTTP status, and a default error
     * description.
     *
     * @param msg A message providing additional context or details about the failure.
     * @param cause The root cause of the exception, if applicable.
     *
     * @see DatabaseFailure
     */
    class Database(msg: String, cause: Throwable? = null) : FindPrincipalByIdException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    /**
     * Represents an encryption-related exception for principal data operations.
     *
     * This exception is used when encryption or decryption operations fail
     * during the handling of principal data. It extends the [FindPrincipalByIdException]
     * to provide additional context specific to encryption failures.
     *
     * @param msg The error message providing details about the failure.
     * @param cause The underlying cause of the exception, if available.
     *
     * @see DatabaseEncryptionFailure
     */
    class Encryption(msg: String, cause: Throwable? = null) : FindPrincipalByIdException(
        msg,
        DatabaseEncryptionFailure.CODE,
        DatabaseEncryptionFailure.STATUS,
        DatabaseEncryptionFailure.DESCRIPTION,
        cause
    )
}
