package io.stereov.singularity.content.core.exception

import io.stereov.singularity.content.invitation.exception.AcceptInvitationException
import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.PostCommitSideEffectFailure
import io.stereov.singularity.global.exception.ResponseMappingFailure
import io.stereov.singularity.global.exception.SingularityException
import io.stereov.singularity.principal.core.exception.UserNotFoundFailure
import org.springframework.http.HttpStatus

sealed class AcceptContentInvitationException(
    msg: String, 
    code: String, 
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : AcceptContentInvitationException(
        msg,
        PostCommitSideEffectFailure.CODE,
        PostCommitSideEffectFailure.STATUS,
        PostCommitSideEffectFailure.DESCRIPTION,
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : AcceptContentInvitationException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    class InvalidInvitation(msg: String, cause: Throwable? = null) : AcceptContentInvitationException(
        msg,
        "INVITATION_TOKEN_INVALID",
        HttpStatus.BAD_REQUEST,
        "Invalid invitation.",
        cause
    )


    class InvitationNotFound(msg: String, cause: Throwable? = null) : AcceptContentInvitationException(
        msg,
        "INVITATION_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "Invitation not found.",
        cause
    )

    class UserNotFound(msg: String, cause: Throwable? = null) : AcceptContentInvitationException(
        msg,
        UserNotFoundFailure.CODE,
        UserNotFoundFailure.STATUS,
        UserNotFoundFailure.DESCRIPTION,
        cause
    )

    class ContentNotFound(msg: String, cause: Throwable? = null) : AcceptContentInvitationException(
        msg,
        "CONTENT_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "Content not found.",
        cause
    )

    class ResponseMapping(msg: String, cause: Throwable? = null) : AcceptContentInvitationException(
        msg,
        ResponseMappingFailure.CODE,
        ResponseMappingFailure.STATUS,
        ResponseMappingFailure.DESCRIPTION,
        cause
    )


    companion object {

        fun from(ex: AcceptInvitationException) = when (ex) {
            is AcceptInvitationException.Database -> Database(ex.message, ex.cause)
            is AcceptInvitationException.InvitationNotFound -> InvitationNotFound(ex.message, ex.cause)
            is AcceptInvitationException.PostCommitSideEffect -> PostCommitSideEffect(ex.message, ex.cause)
        }
    }
}
