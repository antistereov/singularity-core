package io.stereov.singularity.database.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions specific to errors that occur when deleting a document by ID.
 *
 * This sealed class extends [SingularityException], serving as the base for more specific
 * exceptions related to document deletion by identifier. It provides additional context
 * such as an error code, HTTP status, and a description to better encapsulate the failure.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of error.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing additional context about the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class DeleteDocumentByIdException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?,
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents a general database failure during a delete operation by ID.
     *
     * This exception extends [DeleteDocumentByIdException] and is specifically used to
     * encapsulate errors related to database operations that occur while attempting
     * to delete a document by its identifier.
     *
     * @param msg The error message providing details about the failure.
     * @param cause The root cause of the exception, if available.
     *
     * @property code The error code `DATABASE_DELETION_FAILURE`.
     * @property status The associated HTTP status [HttpStatus.INTERNAL_SERVER_ERROR].
     */
    class Database(msg: String, cause: Throwable? = null) : DeleteDocumentByIdException(
        msg,
        "DATABASE_DELETION_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception representing a general failure related to database operations.",
        cause
    )
}
