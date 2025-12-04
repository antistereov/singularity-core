package io.stereov.singularity.content.invitation.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.PostCommitSideEffectFailure
import io.stereov.singularity.email.core.exception.*
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class InviteException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : InviteException(
        msg,
        PostCommitSideEffectFailure.CODE,
        PostCommitSideEffectFailure.STATUS,
        PostCommitSideEffectFailure.DESCRIPTION,
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : InviteException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    class InvitationTokenCreation(msg: String, cause: Throwable? = null) : InviteException(
        msg,
        "INVITATION_TOKEN_CREATION_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception that occurs during the creation of an invitation token.",
        cause
    )

    class Send(msg: String, cause: Throwable? = null) : InviteException(
        msg,
        EmailSendFailure.CODE,
        EmailSendFailure.STATUS,
        EmailSendFailure.DESCRIPTION,
        cause
    )

    class Template(msg: String, cause: Throwable? = null) : InviteException(
        msg,
        EmailTemplateFailure.CODE,
        EmailTemplateFailure.STATUS,
        EmailTemplateFailure.DESCRIPTION,
        cause
    )

    class EmailAuthentication(msg: String, cause: Throwable? = null) : InviteException(
        msg,
        EmailAuthenticationFailure.CODE,
        EmailAuthenticationFailure.STATUS,
        EmailAuthenticationFailure.DESCRIPTION,
        cause
    )

    class EmailDisabled(msg: String, cause: Throwable? = null) : InviteException(
        msg,
        EmailDisabledFailure.CODE,
        EmailDisabledFailure.STATUS,
        EmailDisabledFailure.DESCRIPTION,
        cause
    )

    companion object {

        fun from(ex: EmailException) = when (ex) {
            is EmailException.Send -> Send(ex.message, ex.cause)
            is EmailException.Disabled -> EmailDisabled(ex.message, ex.cause)
            is EmailException.Template -> Template(ex.message, ex.cause)
            is EmailException.Authentication -> EmailAuthentication(ex.message, ex.cause)
        }
    }
}