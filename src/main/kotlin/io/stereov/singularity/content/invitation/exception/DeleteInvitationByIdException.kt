package io.stereov.singularity.content.invitation.exception

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
}