package io.stereov.singularity.auth.core.exception

import io.stereov.singularity.database.encryption.exception.SaveEncryptedDocumentException
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents a sealed class for exceptions related to the user registration process.
 *
 * This class defines various specific exceptions that can occur during the registration
 * process, such as attempts to register an already authenticated user, database failures,
 * or hashing errors. It extends [SingularityException] to provide additional context,
 * including the exception message, error code, HTTP status, detailed description, and an optional
 * root cause.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of error.
 * @param status The HTTP status associated with the exception.
 * @param description A detailed description providing additional context about the exception.
 * @param cause The optional underlying cause of the exception.
 */
sealed class RegisterException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents a specific type of [AuthenticationException] indicating that the user is already authenticated.
     *
     * This exception is typically thrown when there is an attempt to authenticate an already authenticated user.
     *
     * @param msg The exception message providing details about the context of the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `ALREADY_AUTHENTICATED`
     * @property status [HttpStatus.NOT_MODIFIED]
     *
     * @see AuthenticationException.AlreadyAuthenticated
     */
    class AlreadyAuthenticated(msg: String, cause: Throwable? = null) : RegisterException(
        msg,
        "ALREADY_AUTHENTICATED",
        HttpStatus.NOT_MODIFIED,
        "User is already authenticated.",
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
    class Database(msg: String, cause: Throwable? = null) : RegisterException(
        msg,
        "DATABASE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception thrown when an encrypted database operation fails.",
        cause
    )

    /**
     * Represents a specific exception indicating a failure during the hashing process.
     *
     * This exception is thrown when an error occurs while attempting to hash data,
     * such as during password hashing operations. It extends [RegisterException]
     * to provide additional context, including an associated error code, HTTP status,
     * and a detailed description of the issue.
     *
     * @param msg The exception message providing details about the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `HASHING_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     *
     * @see io.stereov.singularity.database.hash.exception.HashException.Hashing
     */
    class Hash(msg: String, cause: Throwable? = null) : RegisterException(
        msg,
        "HASHING_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception thrown when an error occurs during hashing operations.",
        cause
    )

    /**
     * Exception indicating that a post-commit side effect has failed.
     *
     * This exception is a subclass of [RegisterException] and is typically used
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
    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : RegisterException(
        msg,
        "POST_COMMIT_SIDE_EFFECT_FAILURE",
        HttpStatus.MULTI_STATUS,
        "Exception thrown when a post-commit side effect fails.",
        cause
    )
}
