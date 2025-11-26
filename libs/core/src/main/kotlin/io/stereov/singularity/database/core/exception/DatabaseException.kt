package io.stereov.singularity.database.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents a family of exceptions related to database operations.
 *
 * This sealed class extends [SingularityException], introducing specialized
 * exceptions for database-related errors such as missing entities, general database failures,
 * post-commit side effects, and invalid documents stored in the database.
 *
 * @param msg The error message providing details about the failure.
 * @param code A unique error code representing the type of database exception.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing context about the database error.
 * @param cause The root cause of the exception, if available.
 */
sealed class DatabaseException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Exception thrown when a database entity is not found.
     *
     * This exception is a specific type of [DatabaseException].
     *
     * @param msg The error message providing details about the missing entity.
     * @param cause The root cause of this exception, if any.
     *
     * @property code `DATABASE_ENTITY_NOT_FOUND`
     * @property status [HttpStatus.NOT_FOUND]
     */
    class NotFound(msg: String, cause: Throwable? = null): DatabaseException(
        msg,
        "DATABASE_ENTITY_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "Exception thrown when a database entity is not found.",
        cause
    )

    /**
     * Exception representing a general failure related to database operations.
     *
     * This class is a specific type of [DatabaseException].
     *
     * @param msg The error message providing details about the failure.
     * @param cause The root cause of this exception, if any.
     *
     * @property code The code `DATABASE_FAILURE`
     * @property status The status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Database(msg: String, cause: Throwable? = null) : DatabaseException(
        msg,
        "DATABASE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception representing a general failure related to database operations.",
        cause
    )
}
