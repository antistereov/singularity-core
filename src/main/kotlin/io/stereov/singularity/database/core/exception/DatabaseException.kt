package io.stereov.singularity.database.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions related to database operations.
 *
 * This is a sealed class that extends [SingularityException] and serves as the base class
 * for all database-specific exceptions. It provides a common structure to handle errors
 * related to database entities, operations, and post-commit side effects.
 *
 * @param msg The error message describing the issue.
 * @param code The error code identifier.
 * @param cause The underlying cause of the exception, if available.
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

    /**
     * Exception thrown when a database transaction has been successfully committed, but a later
     * side effect, such as publishing an event or updating a cache, fails.
     *
     * Extends [DatabaseException].
     *
     * @param msg The error message providing details about the specific failure.
     * @param cause The underlying cause of the exception, if any.
     *
     * @property code `POST_DATABASE_COMMIT_SIDE_EFFECT_FAILURE`
     * @property status [HttpStatus.OK]
     */
    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : DatabaseException(
        msg,
        "POST_DATABASE_COMMIT_SIDE_EFFECT_FAILURE",
        HttpStatus.OK,
        "Exception thrown when a database transaction has been successfully committed, but a later" +
                " side effect, such as publishing an event or updating a cache, fails.",
        cause
    )

    /**
     * Indicates an invalid document stored in the database.
     *
     * This exception extends [DatabaseException].
     *
     * @param msg The error message providing details about the specific failure.
     * @param cause The underlying cause of the exception, if any.
     *
     * @property code `INVALID_DATABASE_OBJECT`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class InvalidDocument(msg: String, cause: Throwable? = null) : DatabaseException(
        msg,
        "INVALID_DATABASE_OBJECT",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Indicates an invalid document stored in the database.",
        cause
    )
}
