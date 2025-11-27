package io.stereov.singularity.auth.core.exception

import io.stereov.singularity.database.encryption.exception.SaveEncryptedDocumentException
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents a set of exceptions related to email verification functionality.
 *
 * This sealed class is used as the base type for various specific email verification-related
 * exceptions. It extends from [SingularityException] and provides additional common properties
 * such as an error code, HTTP status, and a descriptive message. Exceptions inheriting from
 * `VerifyEmailException` are used to handle specific error cases during the email verification
 * process, such as already verified emails, user not found, database failures, and invalid tokens.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code identifying the specific error type.
 * @param status The HTTP status associated with the error.
 * @param description A detailed description giving more context about the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class VerifyEmailException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents an exception indicating that the user's email has already been verified.
     *
     * This exception is typically thrown when an attempt is made to verify a user's email that is already marked as verified.
     * It extends [VerifyEmailException] to provide specific context, such as an associated error code,
     * HTTP status, description, and an optional cause.
     *
     * @param msg The exception message providing details about the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `EMAIL_ALREADY_VERIFIED`
     * @property status [HttpStatus.NOT_MODIFIED]
     */
    class AlreadyVerified(msg: String, cause: Throwable? = null) : VerifyEmailException(
        msg,
        "EMAIL_ALREADY_VERIFIED",
        HttpStatus.NOT_MODIFIED,
        "User's email is already verified.",
        cause
    )

    /**
     * Represents an exception indicating that a specified user could not be found.
     *
     * This exception is typically thrown when an operation attempts to access or interact with a user
     * that does not exist within the system's database. It extends `VerifyEmailException` and provides
     * additional context, including a unique error code, an associated HTTP status, and a detailed
     * description of the issue.
     *
     * @param msg The error message providing details about the exception.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `USER_NOT_FOUND`
     * @property status [HttpStatus.NOT_FOUND]
     */
    class UserNotFound(msg: String, cause: Throwable? = null) : VerifyEmailException(
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
    class Database(msg: String, cause: Throwable? = null) : VerifyEmailException(
        msg,
        "DATABASE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception thrown when an encrypted database operation fails.",
        cause
    )

    /**
     * Represents an exception indicating an invalid token during the email verification process.
     *
     * This exception is thrown when the provided token does not match the user's verification secret.
     *
     * @param msg The error message describing the context of the exception.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `INVALID_TOKEN`
     * @property status [HttpStatus.UNAUTHORIZED]
     */
    class InvalidToken(msg: String, cause: Throwable? = null) : VerifyEmailException(
        msg,
        "INVALID_TOKEN",
        HttpStatus.UNAUTHORIZED,
        "The provided token does not match the user's verification secret.",
        cause
    )

    /**
     * Exception indicating that a post-commit side effect has failed.
     *
     * This exception is a subclass of [VerifyEmailException] and is typically used
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
    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : VerifyEmailException(
        msg,
        "POST_COMMIT_SIDE_EFFECT_FAILURE",
        HttpStatus.MULTI_STATUS,
        "Exception thrown when a post-commit side effect fails.",
        cause
    )
}
