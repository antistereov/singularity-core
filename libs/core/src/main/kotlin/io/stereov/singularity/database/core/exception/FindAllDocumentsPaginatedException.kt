package io.stereov.singularity.database.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents a family of exceptions that occur when retrieving documents
 * in a paginated manner fails.
 *
 * This sealed class extends [SingularityException] and provides specialized
 * handling for failures related to document retrieval with pagination,
 * including database-related issues.
 *
 * @param msg The error message providing details about the exception.
 * @param code A unique code identifying the type of exception.
 * @param status The HTTP status associated with this exception.
 * @param description A detailed description of the exception.
 * @param cause The root cause of the exception, if any.
 */
sealed class FindAllDocumentsPaginatedException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?,
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents a general failure related to database operations during
     * document retrieval in a paginated manner.
     *
     * This exception is a specific type of [FindAllDocumentsPaginatedException].
     *
     * @param msg A message describing the nature of the database failure.
     * @param cause The underlying cause of the exception, if available.
     *
     * @see DatabaseFailure
     */
    class Database(msg: String, cause: Throwable? = null) : FindAllDocumentsPaginatedException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )
}
