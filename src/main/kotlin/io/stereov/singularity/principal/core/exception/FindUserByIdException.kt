package io.stereov.singularity.principal.core.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.encryption.exception.DatabaseEncryptionFailure
import io.stereov.singularity.database.encryption.exception.FindEncryptedDocumentByIdException
import io.stereov.singularity.global.exception.SingularityException
import io.stereov.singularity.principal.core.model.User
import org.springframework.http.HttpStatus

/**
 * Represents exceptions specifically related to the failure of locating a [User] by its identifier.
 *
 * This sealed class serves as a base for more specific exceptions that may arise when
 * attempting to find or handle a [User] by its unique identifier, such as email or ID.
 * It extends [SingularityException] to provide a consistent structure for error handling,
 * including message, error code, HTTP status, detailed description, and optional underlying cause.
 *
 * @param msg The error message describing the issue in detail.
 * @param code A unique code representing the type of error.
 * @param status The corresponding HTTP status associated with this exception.
 * @param description A detailed description providing contextual information about the error.
 * @param cause The underlying cause of the exception, if any.
 */
sealed class FindUserByIdException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?,
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Exception thrown when a principal cannot be located.
     *
     * This exception is a specific type of [FindUserByIdException] that is used
     * to indicate that no principal with the specified email could be found in the system.
     * It provides consistent error details, including a message, error code, HTTP status,
     * and a default description for the error.
     *
     * @param msg A message providing additional details about the failure.
     * @param cause The underlying cause of the exception, if available.
     *
     * @see UserNotFoundFailure
     */
    class UserNotFound(msg: String, cause: Throwable? = null) : FindUserByIdException(
        msg,
        UserNotFoundFailure.CODE,
        UserNotFoundFailure.STATUS,
        UserNotFoundFailure.DESCRIPTION,
        cause
    )

    /**
     * Exception indicating a failure to retrieve a principal from the database.
     *
     * This exception is a specific type of [FindUserByIdException] used to signal
     * backend database issues when attempting to locate or fetch a principal. It contains
     * consistent error details, such as an error code, HTTP status, and a default error
     * description.
     *
     * @param msg A message providing additional context or details about the failure.
     * @param cause The root cause of the exception, if applicable.
     *
     * @see DatabaseFailure
     */
    class Database(msg: String, cause: Throwable? = null) : FindUserByIdException(
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
     * during the handling of principal data. It extends the [FindUserByIdException]
     * to provide additional context specific to encryption failures.
     *
     * @param msg The error message providing details about the failure.
     * @param cause The underlying cause of the exception, if available.
     *
     * @see DatabaseEncryptionFailure
     */
    class Encryption(msg: String, cause: Throwable? = null) : FindUserByIdException(
        msg,
        DatabaseEncryptionFailure.CODE,
        DatabaseEncryptionFailure.STATUS,
        DatabaseEncryptionFailure.DESCRIPTION,
        cause
    )

    companion object {

        /**
         * Maps an instance of [FindEncryptedDocumentByIdException] to its corresponding
         * [FindUserByIdException] subtype.
         *
         * This function transforms exceptions encountered during encrypted document operations
         * into their equivalent exceptions specific to user operations, preserving the error
         * message and cause.
         *
         * @param ex The [FindEncryptedDocumentByIdException] to be mapped.
         * @return The corresponding [FindUserByIdException] subtype.
         */
        fun from(ex: FindEncryptedDocumentByIdException): FindUserByIdException {
            return when (ex) {
                is FindEncryptedDocumentByIdException.Database -> Database(ex.message, ex.cause)
                is FindEncryptedDocumentByIdException.Encryption -> Encryption(ex.message, ex.cause)
                is FindEncryptedDocumentByIdException.NotFound -> UserNotFound(ex.message, ex.cause)
            }
        }
    }

}
