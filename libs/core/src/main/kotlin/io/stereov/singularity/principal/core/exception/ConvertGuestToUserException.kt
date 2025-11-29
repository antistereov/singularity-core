package io.stereov.singularity.principal.core.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.PostCommitSideEffectFailure
import io.stereov.singularity.database.encryption.exception.SaveEncryptedDocumentException
import io.stereov.singularity.database.hash.exception.HashFailure
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions that may occur during the process of converting a guest to a user.
 *
 * This sealed class serves as the base exception for all types of errors related
 * to guest-to-user conversion operations. It extends the [SingularityException]
 * to provide a consistent structure and additional context for such errors,
 * including a message, error code, HTTP status, detailed description, and optionally
 * a root cause.
 *
 * @param msg The error message providing details about the exception.
 * @param code A unique code representing the specific type of conversion error.
 * @param status The HTTP status associated with the specific error.
 * @param description A detailed description of the error scenario.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class ConvertGuestToUserException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?,
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Exception representing an attempt to convert a guest to a user that is already a user.
     *
     * This exception is thrown when attempting to execute a guest-to-user conversion
     * on an entity that is already identified as a user. It extends the
     * [ConvertGuestToUserException] class to provide a specific error case for handling
     * such scenarios in a consistent manner.
     *
     * @param msg A message describing the details of the error.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `GUEST_IS_ALREADY_USER`
     * @property status [HttpStatus.NOT_MODIFIED]
     */
    class IsAlreadyUser(msg: String, cause: Throwable? = null) : ConvertGuestToUserException(
        msg,
        "GUEST_IS_ALREADY_USER",
        HttpStatus.NOT_MODIFIED,
        "Cannot convert guest to user because it is already a user.",
        cause
    )

    /**
     * Exception representing an attempt to convert a guest user to a standard user
     * where the email address is already associated with an existing account.
     *
     * This exception is a specific type of [ConvertGuestToUserException],
     * designed to handle scenarios where email address conflicts prevent the
     * successful conversion of a guest to a user.
     *
     * @param msg A message describing the details of the error.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `GUEST_EMAIL_TAKEN`
     * @property status [HttpStatus.CONFLICT]
     */
    class EmailTaken(msg: String, cause: Throwable? = null) : ConvertGuestToUserException(
        msg,
        "GUEST_EMAIL_TAKEN",
        HttpStatus.CONFLICT,
        "Cannot convert guest to user because the email address is already taken.",
        cause
    )

    /**
     * Exception representing an error while retrieving a guest user from the database.
     *
     * This exception is thrown when a failure occurs during the process of retrieving
     * guest user information from the database. It extends [ConvertGuestToUserException]
     * and provides detailed context about the error, including a specific error code,
     * HTTP status, and description.
     *
     * @param msg A message providing the details about the error.
     * @param cause The underlying cause of the exception, if available.
     *
     * @see DatabaseFailure
    */
    class Database(msg: String, cause: Throwable? = null) : ConvertGuestToUserException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    /**
     * Exception representing the absence of a guest entity required for conversion to a user.
     *
     * This exception is thrown when an attempt to convert a guest to a user fails because
     * the guest entity does not exist or cannot be found. It extends the `ConvertGuestToUserException`
     * class, providing consistent error structure and details for such cases.
     *
     * @param msg A message describing the details of the error.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `GUEST_NOT_FOUND`
     * @property status [HttpStatus.NOT_FOUND]
     */
    class NotFound(msg: String, cause: Throwable? = null) : ConvertGuestToUserException(
        msg,
        "GUEST_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "Cannot convert guest to user because it does not exist.",
        cause
    )

    /**
     * Exception representing a failure related to hash generation or verification.
     *
     * This exception is thrown when there is an error during the process of generating
     * or verifying a cryptographic hash. It extends the [ConvertGuestToUserException]
     * to represent a specific failure in the context of converting a guest to a user.
     *
     * @param msg The error message providing context for the exception.
     * @param cause The underlying cause of the exception, if available.
     *
     * @property code `GUEST_HASH_FAILURE`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Hash(msg: String, cause: Throwable? = null) : ConvertGuestToUserException(
        msg,
        HashFailure.CODE,
        HashFailure.STATUS,
        HashFailure.DESCRIPTION,
        cause
    )

    /**
     * Exception indicating that a post-commit side effect has failed.
     *
     * This exception is a subclass of [ConvertGuestToUserException] and is typically used
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
    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : ConvertGuestToUserException(
        msg,
        PostCommitSideEffectFailure.CODE,
        PostCommitSideEffectFailure.STATUS,
        PostCommitSideEffectFailure.DESCRIPTION,
        cause
    )
}
