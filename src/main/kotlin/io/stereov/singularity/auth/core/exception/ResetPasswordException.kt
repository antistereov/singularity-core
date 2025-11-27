package io.stereov.singularity.auth.core.exception

import io.stereov.singularity.database.encryption.exception.SaveEncryptedDocumentException
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions related to the reset password operation.
 *
 * This sealed class extends [SingularityException] and provides a structured way to handle
 * various error scenarios that may occur during the password reset process. Each subclass
 * represents a specific type of error with its own unique error code, HTTP status, and description.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the specific type of error.
 * @param status The associated HTTP status for this exception.
 * @param description A detailed description providing context about the exception.
 * @param cause The optional underlying cause of the exception.
 */
sealed class ResetPasswordException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents an exception indicating that a user was not found during a reset password operation.
     *
     * This exception is typically thrown when the system is unable to locate a user with the provided
     * information, such as email or username, during the password reset process.
     *
     * @param msg The exception message providing details about the context of the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `USER_NOT_FOUND`
     * @property status [HttpStatus.NOT_FOUND]
     */
    class UserNotFound(msg: String, cause: Throwable? = null) : ResetPasswordException(
        msg,
        "USER_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "User not found.",
        cause
    )

    /**
     * Represents an exception indicating a failure during an encrypted database operation.
     *
     * This exception is thrown when a database operation encounters an error that prevents it
     * from being completed successfully. This may occur due to issues such as decryption failures,
     * corrupted data, or other internal database-related problems.
     *
     * @param msg A message providing details about the context of the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `DATABASE_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Database(msg: String, cause: Throwable? = null) : ResetPasswordException(
        msg,
        "DATABASE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception thrown when an encrypted database operation fails.",
        cause
    )

    /**
     * Represents an exception indicating an invalid token during the password reset process.
     *
     * This exception is thrown when the provided token does not match the user's verification secret
     * during a reset password operation.
     *
     * It extends the [ResetPasswordException] class and provides additional context, including a
     * specific error code, HTTP status, and a detailed description of the issue.
     *
     * @param msg The exception message describing the context of the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `INVALID_TOKEN`
     * @property status [HttpStatus.UNAUTHORIZED]
     */
    class InvalidToken(msg: String, cause: Throwable? = null) : ResetPasswordException(
        msg,
        "INVALID_TOKEN",
        HttpStatus.UNAUTHORIZED,
        "The provided token does not match the user's verification secret.",
        cause
    )

    /**
     * Represents an exception indicating a failure during hashing operations.
     *
     * This exception is thrown when an error occurs in the process of performing hashing-related operations.
     * It extends [ResetPasswordException] to include relevant context such as a message, error code,
     * HTTP status, a description, and an optional underlying cause.
     *
     * @param msg The error message describing the exception.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `HASH_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Hash(msg: String, cause: Throwable? = null) : ResetPasswordException(
        msg,
        "HASH_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception thrown when an error occurs during hashing operations.",
        cause
    )

    /**
     * Exception indicating that a post-commit side effect has failed.
     *
     * This exception is a subclass of [ResetPasswordException] and is typically used
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
    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : ResetPasswordException(
        msg,
        "POST_COMMIT_SIDE_EFFECT_FAILURE",
        HttpStatus.MULTI_STATUS,
        "Exception thrown when a post-commit side effect fails.",
        cause
    )
}
