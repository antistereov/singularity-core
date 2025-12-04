package io.stereov.singularity.principal.group.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.PostCommitSideEffectFailure
import io.stereov.singularity.database.encryption.exception.SaveEncryptedDocumentException
import io.stereov.singularity.global.exception.SingularityException
import io.stereov.singularity.principal.core.exception.UserNotFoundFailure
import org.springframework.http.HttpStatus

/**
 * Represents exceptions related to group members. This sealed class serves as a base
 * class for more specific exceptions that define particular failure scenarios encountered
 * within the group membership context.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code representing the type of error.
 * @param status The associated HTTP status for the exception.
 * @param description A detailed description providing context about the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class GroupMemberException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable? = null
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Exception used to indicate that a group with the specified key could not be found.
     *
     * This exception extends the [GroupMemberException] to signal failures related to the
     * absence of a group matching the provided key.
     *
     * @param msg A descriptive message explaining the issue encountered.
     * @param cause The optional underlying cause of the exception.
     *
     * @property code `GROUP_NOT_FOUND`
     * @property status [HttpStatus.NOT_FOUND]
     */
    class GroupNotFound(msg: String, cause: Throwable? = null) : GroupMemberException(
        msg,
        "GROUP_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "No group with specified key found.",
        cause
    )

    /**
     * Exception used to indicate that a user with the specified ID could not be found.
     *
     * This exception extends the [GroupMemberException] to signal failures specifically related to
     * the absence of a user matching the provided ID within the group context.
     *
     * @param msg A descriptive message explaining the issue encountered.
     * @param cause The optional underlying cause of the exception.
     *
     * @see UserNotFound
     */
    class UserNotFound(msg: String, cause: Throwable? = null) : GroupMemberException(
        msg,
        UserNotFoundFailure.CODE,
        UserNotFoundFailure.STATUS,
        UserNotFoundFailure.DESCRIPTION,
        cause
    )

    /**
     * Exception used to indicate a failure while attempting to retrieve group member information
     * from the database. This exception extends the [GroupMemberException] to signal database-related
     * issues specifically encountered in the context of group membership.
     *
     * @param msg A descriptive message outlining the nature of the database failure.
     * @param cause The optional underlying cause of the exception.
     *
     * @see DatabaseFailure
     */
    class Database(msg: String, cause: Throwable? = null) : GroupMemberException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    /**
     * Exception indicating that a post-commit side effect has failed.
     *
     * This exception is a subclass of [GroupMemberException] and is typically used
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
    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : GroupMemberException(
        msg,
        PostCommitSideEffectFailure.CODE,
        PostCommitSideEffectFailure.STATUS,
        PostCommitSideEffectFailure.DESCRIPTION,
        cause
    )
}
