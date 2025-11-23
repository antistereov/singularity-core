package io.stereov.singularity.content.core.exception

import io.stereov.singularity.content.core.exception.ContentException.Database.Companion.CODE
import io.stereov.singularity.content.core.exception.ContentException.Database.Companion.STATUS
import io.stereov.singularity.content.core.exception.ContentException.NotAuthorized.Companion.CODE
import io.stereov.singularity.content.core.exception.ContentException.NotAuthorized.Companion.STATUS
import io.stereov.singularity.content.core.exception.ContentException.NotFound.Companion.CODE
import io.stereov.singularity.content.core.exception.ContentException.NotFound.Companion.STATUS
import io.stereov.singularity.content.core.exception.ContentException.PostCommitSideEffect.Companion.CODE
import io.stereov.singularity.content.core.exception.ContentException.PostCommitSideEffect.Companion.STATUS
import io.stereov.singularity.database.core.exception.DatabaseException
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class ContentException(
    msg: String, 
    code: String, 
    status: HttpStatus, 
    cause: Throwable? = null
) : SingularityException(msg, code, status, cause) {

    companion object {

        /**
         * Map a [DatabaseException] to the corresponding [ContentException].
         *
         * @param ex The [DatabaseException]
         * @return Thhe corresponding [ContentException]
         */
        fun fromDatabaseException(ex: DatabaseException): ContentException {
            return when (ex) {
                is DatabaseException.Database -> Database(ex.message ?: ex.code, ex.cause)
                is DatabaseException.NotFound -> NotFound(ex.message ?: ex.code, ex.cause)
                is DatabaseException.PostCommitSideEffect -> PostCommitSideEffect(ex.message ?: ex.code, ex.cause)
            }
        }
    }

    /**
     * Indicates that the content is not accessible by the user that performs the request.
     * Extends [ContentException].
     * 
     * @param msg The error message.
     * @param cause The cause.
     * 
     * @property CODE The error code `CONTENT_ACCESS_NOT_AUTHORIZED`
     * @property STATUS The status [HttpStatus.FORBIDDEN].
     */
    class NotAuthorized(msg: String, cause: Throwable? = null) : ContentException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = "CONTENT_ACCESS_NOT_AUTHORIZED"
            val STATUS = HttpStatus.FORBIDDEN
        }
    }

    /**
     * Exception thrown when a database entity is not found.
     *
     * This exception is a specific type of [ContentException] used to indicate the absence of the
     * requested entity in the database.
     *
     * @param msg The error message providing details about the missing entity.
     * @param cause The root cause of this exception, if any.
     *
     * @property CODE The error code `DATABASE_ENTITY_NOT_FOUND`
     * @property STATUS The status [HttpStatus.NOT_FOUND]
     *
     * @see [DatabaseException.NotFound]
     */
    class NotFound(msg: String, cause: Throwable? = null): ContentException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = DatabaseException.NotFound.CODE
            val STATUS = DatabaseException.NotFound.STATUS
        }
    }

    /**
     * Exception representing a general failure related to database operations.
     *
     * This class is a specific type of [ContentException], used to indicate errors
     * that occur when interacting with the database but do not fit into more specific categories
     * such as missing entities or post-commit operation failures.
     *
     * @param msg The error message providing details about the failure.
     * @param cause The root cause of this exception, if any.
     *
     * @property CODE The code `DATABASE_FAILURE`
     * @property STATUS The status [HttpStatus.INTERNAL_SERVER_ERROR]
     *
     * @see [DatabaseException.Database]
     */
    class Database(msg: String, cause: Throwable? = null) : ContentException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = DatabaseException.Database.CODE
            val STATUS = DatabaseException.Database.STATUS
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
     * Extends [ContentException].
     *
     * @param msg The error message providing details about the specific failure.
     * @param cause The underlying cause of the exception, if any.
     *
     * @property CODE The code `POST_DATABASE_COMMIT_SIDE_EFFECT_FAILURE`
     * @property STATUS The status [HttpStatus.OK]
     *
     * @see [DatabaseException.PostCommitSideEffect]
     */
    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : ContentException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = DatabaseException.PostCommitSideEffect.CODE
            val STATUS = DatabaseException.PostCommitSideEffect.STATUS
        }
    }
}
