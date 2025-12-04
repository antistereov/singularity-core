package io.stereov.singularity.admin.core.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.PostCommitSideEffectFailure
import io.stereov.singularity.database.encryption.exception.SaveEncryptedDocumentException
import io.stereov.singularity.database.hash.exception.HashFailure
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class InitRootAccountException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Exception representing a failure in generating or verifying a hash.
     *
     * @param msg A message describing the details of the error.
     * @param cause The underlying cause of the exception, if available.
     *
     * @see HashFailure
     */
    class Hash(msg: String, cause: Throwable? = null) : InitRootAccountException(
        msg,
        HashFailure.CODE,
        HashFailure.STATUS,
        HashFailure.DESCRIPTION,
        cause
    )

    /**
     * Exception thrown when an encrypted database operation fails.
     *
     * This exception is a subclass of [SaveEncryptedDocumentException].
     *
     * @param msg The error message providing details about the failure.
     * @param cause The underlying cause of this exception, if any.
     *
     * @see DatabaseFailure
     *
     * @see SaveEncryptedDocumentException.Database
     */
    class Database(msg: String, cause: Throwable? = null) : InitRootAccountException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    /**
     * Exception indicating that a post-commit side effect has failed.
     *
     * This exception is a subclass of [InitRootAccountException] and is typically used
     * to signal failure in executing operations that occur as a side effect following
     * the successful commitment of a primary operation.
     *
     * @param msg A message providing details about the specific failure.
     * @param cause The underlying cause of this exception, if available.
     *
     * @see PostCommitSideEffect
     *
     * @see SaveEncryptedDocumentException.PostCommitSideEffect
     */
    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : InitRootAccountException(
        msg,
        PostCommitSideEffectFailure.CODE,
        PostCommitSideEffectFailure.STATUS,
        PostCommitSideEffectFailure.DESCRIPTION,
        cause
    )
}
