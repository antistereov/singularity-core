package io.stereov.singularity.principal.settings.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.PostCommitSideEffectFailure
import io.stereov.singularity.database.encryption.exception.SaveEncryptedDocumentException
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions that may occur during the process of changing a user's email address.
 *
 * This sealed class serves as the base class for specific exceptions related to changing
 * emails within the application. It provides a structured way to capture and handle different
 * error scenarios that may arise in this operation.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of error.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing more context about the error.
 * @param cause The optional underlying cause of the exception.
 */
sealed class ChangeEmailException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?,
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents an exception indicating that the provided email address is already in use.
     *
     * This exception is thrown during an email change operation when the specified email
     * address is found to be associated with an existing account.
     *
     * @param msg A message providing details about the context of the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `EMAIL_TAKEN`
     * @property status [HttpStatus.CONFLICT]
     */
    class EmailTaken(msg: String, cause: Throwable? = null) : ChangeEmailException(
        msg,
        "EMAIL_TAKEN",
        HttpStatus.CONFLICT,
        "Email address is already taken.",
        cause
    )

    /**
     * Represents an exception indicating a failure during database operations related to changing the user's email.
     *
     * This exception is thrown when the system encounters an error that prevents it from successfully updating
     * the user's email information in the database. This can occur due to connectivity issues, conflicts,
     * or other internal database-related errors.
     *
     * @param msg A message providing details about the context of the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @see DatabaseFailure
     */
    class Database(msg: String, cause: Throwable? = null) : ChangeEmailException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    /**
     * Exception indicating that a post-commit side effect has failed.
     *
     * This exception is a subclass of [ChangeEmailException] and is typically used
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
    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : ChangeEmailException(
        msg,
        PostCommitSideEffectFailure.CODE,
        PostCommitSideEffectFailure.STATUS,
        PostCommitSideEffectFailure.DESCRIPTION,
        cause
    )
}
