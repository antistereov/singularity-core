package io.stereov.singularity.database.encryption.exception

import io.stereov.singularity.database.core.exception.DatabaseEntityNotFound
import io.stereov.singularity.database.core.exception.DatabaseException
import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions specific to encrypted database operations.
 *
 * This is a sealed class extending [SingularityException], serving as the base class
 * for all exceptions related to encrypted database processes and operations.
 *
 * @param msg The error message describing the issue encountered.
 * @param code The error code representing this particular exception.
 * @param status The HTTP status associated with the exception.
 * @param description A description providing additional context about the exception.
 * @param cause The underlying cause of the exception, if applicable.
 *
 * @see DatabaseException
 */
sealed class DeleteEncryptedDocumentByIdException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Exception thrown when an encrypted database operation fails.
     *
     * This exception is a subclass of [DeleteEncryptedDocumentByIdException].
     *
     * @param msg The error message providing details about the failure.
     * @param cause The underlying cause of this exception, if any.
     *
     * @see DatabaseFailure
     *
     * @see DatabaseException.Database
     */
    class Database(msg: String, cause: Throwable? = null) : DeleteEncryptedDocumentByIdException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    class NotFound(msg: String, cause: Throwable? = null) : DeleteEncryptedDocumentByIdException(
        msg,
        DatabaseEntityNotFound.CODE,
        DatabaseEntityNotFound.STATUS,
        DatabaseEntityNotFound.DESCRIPTION,
        cause
    )
}
