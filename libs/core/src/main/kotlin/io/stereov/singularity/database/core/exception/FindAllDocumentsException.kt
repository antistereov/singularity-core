package io.stereov.singularity.database.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions related to the failure of retrieving all documents.
 *
 * This sealed class serves as the base for specific exceptions that occur
 * during the operation of retrieving all documents. It extends [SingularityException],
 * allowing additional context such as error code, HTTP status, detailed description,
 * and an optional throwable cause.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of error.
 * @param status The HTTP status associated with the exception.
 * @param description A description providing more context about the error.
 * @param cause The root cause of the exception, if available.
 */
sealed class FindAllDocumentsException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?,
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents a general database failure exception for all document retrieval operations.
     *
     * This exception is a specialized type of [FindAllDocumentsException] and is used to
     * indicate a general failure during interactions with the database, such as connectivity
     * issues, unexpected errors, or operation failures.
     *
     * @param msg The error message providing details about the failure.
     * @param cause The root cause of this exception, if any.
     *
     * @property code The error code `DATABASE_FAILURE`.
     * @property status The HTTP status [HttpStatus.INTERNAL_SERVER_ERROR].
     */
    class Database(msg: String, cause: Throwable? = null) : FindAllDocumentsException(
        msg,
        "DATABASE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception representing a general failure related to database operations.",
        cause
    )
}
