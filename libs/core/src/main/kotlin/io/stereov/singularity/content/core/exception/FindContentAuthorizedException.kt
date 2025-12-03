package io.stereov.singularity.content.core.exception

import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.FindDocumentByKeyException
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class FindContentAuthorizedException(
    msg: String, 
    code: String, 
    status: HttpStatus,
    description: String,
    cause: Throwable? = null
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Indicates that the content is not accessible by the user that performs the request.
     * Extends [FindContentAuthorizedException].
     * 
     * @param msg The error message.
     * @param cause The cause.
     * 
     * @see NotAuthorizedFailure
     */
    class NotAuthorized(msg: String, cause: Throwable? = null) : FindContentAuthorizedException(
        msg,
        NotAuthorizedFailure.CODE,
        NotAuthorizedFailure.STATUS,
        NotAuthorizedFailure.DESCRIPTION,
        cause
    )

    /**
     * Exception thrown when a database entity is not found.
     *
     * This exception is a specific type of [FindContentAuthorizedException] used to indicate the absence of the
     * requested entity in the database.
     *
     * @param msg The error message providing details about the missing entity.
     * @param cause The root cause of this exception, if any.
     *
     * @see ContentNotFoundFailure
     */
    class NotFound(msg: String, cause: Throwable? = null): FindContentAuthorizedException(
        msg,
        ContentNotFoundFailure.CODE,
        ContentNotFoundFailure.STATUS,
        ContentNotFoundFailure.DESCRIPTION,
        cause
    )

    /**
     * Exception representing a general failure related to database operations.
     *
     * This class is a specific type of [FindContentAuthorizedException], used to indicate errors
     * that occur when interacting with the database but do not fit into more specific categories
     * such as missing entities or post-commit operation failures.
     *
     * @param msg The error message providing details about the failure.
     * @param cause The root cause of this exception, if any.
     *
     * @see DatabaseFailure
     */
    class Database(msg: String, cause: Throwable? = null) : FindContentAuthorizedException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    class NotAuthenticated(msg: String, cause: Throwable? = null) : FindContentAuthorizedException(
        msg,
        AuthenticationException.AuthenticationRequired.CODE,
        AuthenticationException.AuthenticationRequired.STATUS,
        AuthenticationException.AuthenticationRequired.DESCRIPTION,
        cause
    )

    companion object {

        fun from(ex: FindDocumentByKeyException): FindContentAuthorizedException {
            return when (ex) {
                is FindDocumentByKeyException.Database -> Database(ex.message, ex.cause)
                is FindDocumentByKeyException.NotFound -> NotFound(ex.message, ex.cause)
            }
        }

        fun from(ex: ContentException): FindContentAuthorizedException {
            return when (ex) {
                is ContentException.NotAuthorized -> NotAuthorized(ex.message, ex.cause)
                is ContentException.NotAuthenticated -> NotAuthenticated(ex.message, ex.cause)
            }
        }
    }
}
