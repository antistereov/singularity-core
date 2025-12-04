package io.stereov.singularity.auth.oauth2.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.PostCommitSideEffectFailure
import io.stereov.singularity.database.hash.exception.HashFailure
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class SetPasswordException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class PasswordAlreadySet(msg: String, cause: Throwable? = null) : SetPasswordException(
        msg,
        "PASSWORD_ALREADY_SET",
        HttpStatus.NOT_MODIFIED,
        "Password is already set.",
        cause
    )

    /**
     * Represents an exception specific to hashing-related failures during password setting operations.
     *
     * This exception is a specialized type of [SetPasswordException], designed to signal issues that
     * occur when hashing a password. It carries a predefined error code, HTTP status, and description
     * that provides additional context about the nature of the failure.
     *
     * @param msg A descriptive message about the hashing failure.
     * @param cause The underlying cause of the exception, if available (optional).
     *
     * @see HashFailure
     */
    class Hash(msg: String, cause: Throwable? = null) : SetPasswordException(
        msg,
        HashFailure.CODE,
        HashFailure.STATUS,
        HashFailure.DESCRIPTION,
        cause
    )

    /**
     * Represents an exception that indicates a failure in accessing or interacting with the database
     * during the set password operation.
     *
     * This exception extends [SetPasswordException], inheriting its structure to ensure consistent
     * handling of database-related errors that occur in the context of setting a password. It provides
     * a specific error code, status, and description as defined in [DatabaseFailure].
     *
     * This could be used in scenarios such as failure to retrieve or update data in a database, or
     * other unexpected issues occurring at the database interaction level during password operations.
     *
     * @param msg A descriptive message providing context about the error.
     * @param cause The underlying cause of the exception, if available.
     *
     * @see DatabaseFailure
     */
    class Database(msg: String, cause: Throwable? = null) : SetPasswordException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    /**
     * Represents an exception that occurs due to a failure in applying side effects after a commit action has completed.
     *
     * This exception extends [SetPasswordException], inheriting its structure for consistent exception handling.
     * It is intended to signal errors related to post-commit operations, often used in scenarios where side effects
     * such as event propagation or cleanup activities fail after a transactional commit.
     *
     * The exception provides a specific code, status, and description defined in [PostCommitSideEffectFailure].
     *
     * @param msg A descriptive message providing context about the error.
     * @param cause The underlying cause of the exception, if available.
     *
     * @see PostCommitSideEffectFailure
     */
    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : SetPasswordException(
        msg,
        PostCommitSideEffectFailure.CODE,
        PostCommitSideEffectFailure.STATUS,
        PostCommitSideEffectFailure.DESCRIPTION,
        cause
    )
}