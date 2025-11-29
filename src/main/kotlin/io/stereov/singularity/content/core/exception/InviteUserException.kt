package io.stereov.singularity.content.core.exception

import io.stereov.singularity.content.invitation.exception.InviteException
import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.InvalidDocumentFailure
import io.stereov.singularity.database.core.exception.PostCommitSideEffectFailure
import io.stereov.singularity.email.core.exception.EmailAuthenticationFailure
import io.stereov.singularity.email.core.exception.EmailDisabledFailure
import io.stereov.singularity.email.core.exception.EmailSendFailure
import io.stereov.singularity.email.core.exception.EmailTemplateFailure
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class InviteUserException(
    msg: String, 
    code: String, 
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class NotAuthorized(msg: String, cause: Throwable? = null) : InviteUserException(
        msg,
        NotAuthorizedFailure.CODE,
        NotAuthorizedFailure.STATUS,
        NotAuthorizedFailure.DESCRIPTION,
        cause
    )

    class ContentNotFound(msg: String, cause: Throwable? = null): InviteUserException(
        msg,
        ContentNotFoundFailure.CODE,
        ContentNotFoundFailure.STATUS,
        ContentNotFoundFailure.DESCRIPTION,
        cause
    )

    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : InviteUserException(
        msg,
        PostCommitSideEffectFailure.CODE,
        PostCommitSideEffectFailure.STATUS,
        PostCommitSideEffectFailure.DESCRIPTION,
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : InviteUserException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    class InvitationTokenCreation(msg: String, cause: Throwable? = null) : InviteUserException(
        msg,
        "INVITATION_TOKEN_CREATION_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Represents an exception that occurs during the creation of an invitation token.",
        cause
    )

    class Send(msg: String, cause: Throwable? = null) : InviteUserException(
        msg,
        EmailSendFailure.CODE,
        EmailSendFailure.STATUS,
        EmailSendFailure.DESCRIPTION,
        cause
    )

    class Template(msg: String, cause: Throwable? = null) : InviteUserException(
        msg,
        EmailTemplateFailure.CODE,
        EmailTemplateFailure.STATUS,
        EmailTemplateFailure.DESCRIPTION,
        cause
    )

    class EmailAuthentication(msg: String, cause: Throwable? = null) : InviteUserException(
        msg,
        EmailAuthenticationFailure.CODE,
        EmailAuthenticationFailure.STATUS,
        EmailAuthenticationFailure.DESCRIPTION,
        cause
    )

    class EmailDisabled(msg: String, cause: Throwable? = null) : InviteUserException(
        msg,
        EmailDisabledFailure.CODE,
        EmailDisabledFailure.STATUS,
        EmailDisabledFailure.DESCRIPTION,
        cause
    )

    class InvalidDocument(msg: String, cause: Throwable? = null) : InviteUserException(
        msg,
        InvalidDocumentFailure.CODE,
        InvalidDocumentFailure.STATUS,
        InvalidDocumentFailure.DESCRIPTION,
        cause
    )


    companion object {

        fun from(ex: InviteException) = when (ex) {
            is InviteException.Send -> Send(ex.message, ex.cause)
            is InviteException.EmailDisabled -> EmailDisabled(ex.message, ex.cause)
            is InviteException.Template -> Template(ex.message, ex.cause)
            is InviteException.EmailAuthentication -> EmailAuthentication(ex.message, ex.cause)
            is InviteException.Database -> Database(ex.message, ex.cause)
            is InviteException.InvitationTokenCreation -> InvitationTokenCreation(ex.message, ex.cause)
            is InviteException.PostCommitSideEffect -> PostCommitSideEffect(ex.message, ex.cause)
        }

        fun from(ex: FindContentAuthorizedException): InviteUserException {
            return when (ex) {
                is FindContentAuthorizedException.Database -> Database(ex.message, ex.cause)
                is FindContentAuthorizedException.NotFound -> ContentNotFound(ex.message, ex.cause)
                is FindContentAuthorizedException.NotAuthorized -> NotAuthorized(ex.message, ex.cause)
            }
        }

        fun from(ex: GenerateExtendedContentAccessDetailsException): InviteUserException {
            return when (ex) {
                is GenerateExtendedContentAccessDetailsException.Database -> Database(ex.message, ex.cause)
                is GenerateExtendedContentAccessDetailsException.NotAuthorized -> NotAuthorized(ex.message, ex.cause)
                is GenerateExtendedContentAccessDetailsException.ContentNotFound -> ContentNotFound(ex.message, ex.cause)
                is GenerateExtendedContentAccessDetailsException.InvalidDocument -> InvalidDocument(ex.message, ex.cause)
            }
        }
    }
}
