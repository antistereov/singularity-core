package io.stereov.singularity.content.invitation.exception

import io.stereov.singularity.content.core.exception.DeleteContentInvitationException
import io.stereov.singularity.content.core.exception.NotAuthorizedFailure
import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.PostCommitSideEffectFailure
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class DeleteInvitationByIdException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : DeleteInvitationByIdException(
        msg,
        PostCommitSideEffectFailure.CODE,
        PostCommitSideEffectFailure.STATUS,
        PostCommitSideEffectFailure.DESCRIPTION,
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : DeleteInvitationByIdException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    class InvitationNotFound(msg: String, cause: Throwable? = null) : DeleteInvitationByIdException(
        msg,
        "INVITATION_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "Invitation not found.",
        cause
    )

    class InvalidInvitation(msg: String, cause: Throwable? = null) : DeleteInvitationByIdException(
        msg,
        "INVALID_INVITATION_DOCUMENT",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Indicates that the principal document associated with the invitation token is invalid.",
        cause
    )

    class ContentTypeNotFound(msg: String, cause: Throwable? = null) : DeleteInvitationByIdException(
        msg,
        "CONTENT_TYPE_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "Content type not found.",
        cause
    )

    class NotAuthorized(msg: String, cause: Throwable? = null) : DeleteInvitationByIdException(
        msg,
        NotAuthorizedFailure.CODE,
        NotAuthorizedFailure.STATUS,
        NotAuthorizedFailure.DESCRIPTION,
        cause
    )

    companion object {
        fun from(ex: DeleteContentInvitationException) = when (ex) {
            is DeleteContentInvitationException.ContentNotFound -> ContentTypeNotFound(ex.message, ex.cause)
            is DeleteContentInvitationException.Database -> Database(ex.message, ex.cause)
            is DeleteContentInvitationException.NotAuthorized -> NotAuthorized(ex.message, ex.cause)
        }
    }
}