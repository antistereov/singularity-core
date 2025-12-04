package io.stereov.singularity.principal.settings.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.PostCommitSideEffectFailure
import io.stereov.singularity.database.encryption.exception.SaveEncryptedDocumentException
import io.stereov.singularity.file.core.exception.FileFailure
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents exceptions that may occur during the process of deleting a user's avatar.
 *
 * This sealed class serves as the base class for specific exceptions related to deleting
 * user avatars within the application. It provides a structured way to capture and handle
 * different error scenarios that may arise in this operation.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of error.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing more context about the error.
 * @param cause The optional underlying cause of the exception.
 */
sealed class DeleteUserAvatarException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Represents an exception indicating a failure during file operations related to user avatar management.
     *
     * This exception is thrown when an attempt to save a user avatar fails due to file-related issues.
     *
     * @param msg A message providing details about the context of the error.
     * @param cause The optional underlying cause of the exception.
     *
     * @see FileFailure
     */
    class File(msg: String, cause: Throwable? = null) : DeleteUserAvatarException(
        msg,
        FileFailure.CODE,
        FileFailure.STATUS,
        FileFailure.DESCRIPTION,
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
     * @see DatabaseFailure
     */
    class Database(msg: String, cause: Throwable? = null) : DeleteUserAvatarException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    /**
     * Exception indicating that a post-commit side effect has failed.
     *
     * This exception is a subclass of [DeleteUserAvatarException] and is typically used
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
    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : DeleteUserAvatarException(
        msg,
        PostCommitSideEffectFailure.CODE,
        PostCommitSideEffectFailure.STATUS,
        PostCommitSideEffectFailure.DESCRIPTION,
        cause
    )
}
