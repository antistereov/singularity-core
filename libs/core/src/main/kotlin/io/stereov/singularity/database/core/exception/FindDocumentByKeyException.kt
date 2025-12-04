package io.stereov.singularity.database.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions that may occur during the process of finding a group by its key.
 * This sealed class serves as a base for more specific exceptions that handle particular
 * failure scenarios related to this operation.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of error.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing more context about the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class FindDocumentByKeyException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable? = null
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Exception used to indicate that a group with the specified key was not found.
     *
     * This exception extends [FindDocumentByKeyException] and is specifically intended to represent
     * scenarios where the requested group cannot be located in the system.
     *
     * @param msg A descriptive message providing context for the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @see DatabaseEntityNotFound
     */
    class NotFound(msg: String, cause: Throwable? = null) : FindDocumentByKeyException(
        msg,
        DatabaseEntityNotFound.CODE,
        DatabaseEntityNotFound.STATUS,
        DatabaseEntityNotFound.DESCRIPTION,
        cause
    )

    /**
     * Represents a database-related exception occurring when attempting to retrieve a group.
     *
     * This exception is a specific type of [FindDocumentByKeyException] intended to indicate failures
     * that are directly related to interactions with the database, such as connectivity issues or
     * data retrieval failures.
     *
     * @param msg A descriptive message providing additional context about the error.
     * @param cause The underlying cause of the exception, if available.
     *
     * @see DatabaseFailure
     */
    class Database(msg: String, cause: Throwable? = null) : FindDocumentByKeyException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )
}
