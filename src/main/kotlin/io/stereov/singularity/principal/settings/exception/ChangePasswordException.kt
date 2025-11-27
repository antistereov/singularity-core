package io.stereov.singularity.principal.settings.exception

import io.stereov.singularity.database.encryption.exception.SaveEncryptedDocumentException
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions that may occur during the process of changing a user's password.
 *
 * This sealed class serves as the base class for specific exceptions related to password
 * changes within the application. It provides a structured way to capture and handle various
 * error scenarios that may arise in this operation.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of error.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing more context about the error.
 * @param cause The optional underlying cause of the exception.
 */
sealed class ChangePasswordException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?,
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents an exception indicating that a user does not have a password set.
     *
     * This exception is thrown during password change operations when the system detects
     * that the user attempting to change their password has no existing password set on their account.
     *
     * @param msg A message providing details about the context of the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `NO_PASSWORD_SET`
     * @property status [HttpStatus.BAD_REQUEST]
     */
    class NoPasswordSet(msg: String, cause: Throwable? = null) : ChangePasswordException(
        msg,
        "NO_PASSWORD_SET",
        HttpStatus.BAD_REQUEST,
        "User does not have a password set.",
        cause
    )

    /**
     * Represents an exception indicating a failure during database operations related to a password change.
     *
     * This exception is thrown when the system encounters an issue that prevents it from successfully
     * updating the user document in the database. This may occur due to internal database errors,
     * connectivity issues, or other related problems.
     *
     * @param msg A message providing details about the context of the failure.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `DATABASE_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Database(msg: String, cause: Throwable? = null) : ChangePasswordException(
        msg,
        "DATABASE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to update user document in database.",
        cause
    )

    /**
     * Represents an exception indicating a failure to hash a user's password.
     *
     * This exception is thrown when the system encounters an error during
     * the password hashing process, which is a critical security operation
     * for securing user credentials.
     *
     * @param msg The message providing details about the context of the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `HASHING_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Hashing(msg: String, cause: Throwable? = null) : ChangePasswordException(
        msg,
        "HASHING_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to hash user password.",
        cause
    )

    /**
     * Exception indicating that a post-commit side effect has failed.
     *
     * This exception is a subclass of [ChangePasswordException] and is typically used
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
    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : ChangePasswordException(
        msg,
        "POST_COMMIT_SIDE_EFFECT_FAILURE",
        HttpStatus.MULTI_STATUS,
        "Exception thrown when a post-commit side effect fails.",
        cause
    )
}
