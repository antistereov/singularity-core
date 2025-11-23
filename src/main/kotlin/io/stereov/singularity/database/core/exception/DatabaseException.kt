package io.stereov.singularity.database.core.exception

import io.stereov.singularity.database.core.exception.DatabaseException.Database.Companion.CODE
import io.stereov.singularity.database.core.exception.DatabaseException.Database.Companion.STATUS
import io.stereov.singularity.database.core.exception.DatabaseException.NotFound.Companion.CODE
import io.stereov.singularity.database.core.exception.DatabaseException.NotFound.Companion.STATUS
import io.stereov.singularity.database.core.exception.DatabaseException.PostCommitSideEffect.Companion.CODE
import io.stereov.singularity.database.core.exception.DatabaseException.PostCommitSideEffect.Companion.STATUS
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
    cause: Throwable?
) : SingularityException(msg, code, status, cause) {

    /**
     * Exception thrown when a database entity is not found.
     *
     * This exception is a specific type of [DatabaseException] used to indicate the absence of the
     * requested entity in the database.
     *
     * @param msg The error message providing details about the missing entity.
     * @param cause The root cause of this exception, if any.
     *
     * @property CODE The error code `DATABASE_ENTITY_NOT_FOUND`
     * @property STATUS The status [HttpStatus.NOT_FOUND]
     */
    class NotFound(msg: String, cause: Throwable? = null): DatabaseException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = "DATABASE_ENTITY_NOT_FOUND"
            val STATUS = HttpStatus.NOT_FOUND
        }
    }

    /**
     * Exception representing a general failure related to database operations.
     *
     * This class is a specific type of [DatabaseException], used to indicate errors
     * that occur when interacting with the database but do not fit into more specific categories
     * such as missing entities or post-commit operation failures.
     *
     * @param msg The error message providing details about the failure.
     * @param cause The root cause of this exception, if any.
     *
     * @property CODE The code `DATABASE_FAILURE`
     * @property STATUS The status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Database(msg: String, cause: Throwable? = null) : DatabaseException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = "DATABASE_FAILURE"
            val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
        }
    }

    /**
     * Exception thrown when a database transaction has been successfully committed, but a subsequent
     * side effect, such as publishing an event or updating a cache, fails.
     *
     * This class handles failures that occur after the core transaction has been successfully
     * applied to the database. It helps identify problems that relate to post-commit
     * operations, differentiating them from other database-related issues.
     *
     * Extends [DatabaseException].
     *
     * @param msg The error message providing details about the specific failure.
     * @param cause The underlying cause of the exception, if any.
     *
     * @property CODE The code `POST_DATABASE_COMMIT_SIDE_EFFECT_FAILURE`
     * @property STATUS The status [HttpStatus.OK]
     */
    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : DatabaseException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = "POST_DATABASE_COMMIT_SIDE_EFFECT_FAILURE"
            val STATUS = HttpStatus.OK
        }
    }
}
