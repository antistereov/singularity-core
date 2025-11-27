package io.stereov.singularity.database.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents a family of exceptions related to deleting all documents.
 *
 * This sealed class extends [SingularityException], introducing a specialized exception
 * for scenarios where an error occurs during the operation of deleting all documents.
 *
 * @param msg The error message providing details about the failure.
 * @param code A unique error code representing the type of exception.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing context about the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class DeleteAllDocumentsException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?,
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents a database-related failure during document deletion operations.
     *
     * This exception extends [DeleteAllDocumentsException] and is used to indicate
     * general failures that occur within the database system during the deletion
     * of all documents.
     *
     * @param msg The error message providing details about the failure.
     * @param cause The root cause of the exception, if available.
     *
     * @property code `DATABASE_DELETION_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Database(msg: String, cause: Throwable? = null) : DeleteAllDocumentsException(
        msg,
        "DATABASE_DELETION_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception representing a general failure related to database operations.",
        cause
    )
}
