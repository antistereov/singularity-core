package io.stereov.singularity.database.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Exception representing a failure scenario where a document already exists by its identifier.
 *
 * This sealed class extends [SingularityException], serving as a specialized base exception
 * for handling the case where a document with a specific identifier exists when it should not.
 * It adds contextual information such as error messages, codes, HTTP statuses, and descriptions
 * pertaining to these scenarios.
 *
 * @param msg The error message providing details about the specific failure.
 * @param code A unique error code representing the type of exception.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing context for the failure scenario.
 * @param cause The underlying cause of the exception, if applicable.
 */
sealed class DeleteDocumentByKeyException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?,
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents an exception for general database failures.
     *
     * This exception is a subtype of [DeleteDocumentByKeyException] and is used to indicate
     * issues related to database operations that result in an internal server error.
     *
     * @param msg The error message describing the database failure.
     * @param cause The root cause of this exception, if applicable.
     *
     * @see DatabaseFailure
     */
    class Database(msg: String, cause: Throwable? = null) : DeleteDocumentByKeyException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )
}
