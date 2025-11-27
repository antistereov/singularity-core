package io.stereov.singularity.admin.core.exception

import io.stereov.singularity.database.encryption.exception.SaveEncryptedDocumentException
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
     * @property code `USER_HASH_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class HashFailure(msg: String, cause: Throwable? = null) : InitRootAccountException(
        msg,
        "USER_HASH_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to generate or verify hash.",
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
     * @property code `DATABASE_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     *
     * @see SaveEncryptedDocumentException.Database
     */
    class Database(msg: String, cause: Throwable? = null) : InitRootAccountException(
        msg,
        "DATABASE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception thrown when an encrypted database operation fails.",
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
     * @property code `POST_COMMIT_SIDE_EFFECT_FAILURE`
     * @property status [HttpStatus.MULTI_STATUS]
     *
     * @see SaveEncryptedDocumentException.PostCommitSideEffect
     */
    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : InitRootAccountException(
        msg,
        "POST_COMMIT_SIDE_EFFECT_FAILURE",
        HttpStatus.MULTI_STATUS,
        "Exception thrown when a post-commit side effect fails.",
        cause
    )
}
