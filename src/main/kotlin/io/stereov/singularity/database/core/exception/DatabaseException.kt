package io.stereov.singularity.database.core.exception

import io.stereov.singularity.global.exception.SingularityException

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
    cause: Throwable?
) : SingularityException(msg, code, cause) {

    /**
     * Exception thrown when a database entity is not found.
     *
     * This exception is a specific type of [DatabaseException] used to indicate the absence of the
     * requested entity in the database.
     *
     * @param msg The error message providing details about the missing entity.
     * @param cause The root cause of this exception, if any.
     */
    class NotFound(msg: String, cause: Throwable? = null): DatabaseException(msg, CODE, cause) {
        companion object { const val CODE = "DATABASE_ENTITY_NOT_FOUND" }
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
     */
    class Database(msg: String, cause: Throwable? = null) : DatabaseException(msg, CODE, cause) {
        companion object { const val CODE = "DATABASE_FAILURE" }
    }

    /**
     * Exception thrown when a database transaction has been successfully committed, but a subsequent
     * side effect, such as publishing an event or updating a cache, fails.
     *
     * This class handles failures that occur after the core transaction has been successfully
     * applied to the database. It helps identify problems that relate to post-commit
     * operations, differentiating them from other database-related issues.
     *
     * @param msg The error message providing details about the specific failure.
     * @param cause The underlying cause of the exception, if any.
     */
    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : DatabaseException(msg, CODE, cause) {
        companion object { const val CODE = "POST_DATABASE_COMMIT_SIDE_EFFECT_FAILURE" }
    }
}