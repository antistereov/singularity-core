package io.stereov.singularity.database.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions related to the failure of finding a document by ID.
 *
 * This sealed class serves as the base for specialized exceptions that occur during
 * the process of retrieving a document by its ID. It extends [SingularityException],
 * providing additional context, such as a unique error code, HTTP status, a detailed
 * description, and an optional cause of the exception.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of error.
 * @param status The HTTP status associated with the exception.
 * @param description A description providing more context about the error.
 * @param cause The root cause of the exception, if available.
 */
sealed class FindDocumentByIdException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?,
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents a general database failure exception related to document retrieval by ID.
     *
     * This exception is a specialized type of [FindDocumentByIdException], indicating
     * a general failure in database operations, such as connectivity issues, transaction errors,
     * or unexpected operational failures, when attempting to find a document by its ID.
     *
     * @param msg The error message providing details about the failure.
     * @param cause The root cause of this exception, if any.
     *
     * @property code The error code `DATABASE_FAILURE`.
     * @property status The HTTP status [HttpStatus.INTERNAL_SERVER_ERROR].
     */
    class Database(msg: String, cause: Throwable? = null) : FindDocumentByIdException(
        msg,
        "DATABASE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception representing a general failure related to database operations.",
        cause
    )

    /**
     * Exception indicating that a database entity could not be found.
     *
     * This class extends [FindDocumentByIdException], specifically representing
     * situations where an entity within a database, identified by a unique key,
     * is missing or does not exist.
     *
     * @param msg The error message describing the missing entity.
     * @param cause The root cause of this exception, if any.
     *
     * @property code The error code `DOCUMENT_NOT_FOUND`.
     * @property status The associated HTTP status [HttpStatus.NOT_FOUND].
     */
    class NotFound(msg: String, cause: Throwable? = null) : FindDocumentByIdException(
        msg,
        "DOCUMENT_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "Exception thrown when a database entity is not found.",
        cause
    )
}
