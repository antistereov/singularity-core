package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.PostCommitSideEffectFailure
import io.stereov.singularity.email.core.exception.*
import io.stereov.singularity.global.exception.SingularityException
import io.stereov.singularity.principal.core.exception.InvalidUserDocumentFailure
import io.stereov.singularity.principal.core.exception.NoPasswordProvider
import org.springframework.http.HttpStatus

sealed class SendEmailAuthenticationException (
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : SendEmailAuthenticationException(
        msg,
        PostCommitSideEffectFailure.CODE,
        PostCommitSideEffectFailure.STATUS,
        PostCommitSideEffectFailure.DESCRIPTION,
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : SendEmailAuthenticationException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    class NoPasswordSet(msg: String, cause: Throwable? = null) : SendEmailAuthenticationException(
        msg,
        NoPasswordProvider.CODE,
        NoPasswordProvider.STATUS,
        NoPasswordProvider.DESCRIPTION,
        cause
    )

    class InvalidDocument(msg: String, cause: Throwable? = null) : SendEmailAuthenticationException(
        msg,
        InvalidUserDocumentFailure.CODE,
        InvalidUserDocumentFailure.STATUS,
        InvalidUserDocumentFailure.DESCRIPTION,
        cause
    )

    class CooldownCache(msg: String, cause: Throwable? = null) : SendEmailAuthenticationException(
        msg,
        EmailCooldownCacheFailure.CODE,
        EmailCooldownCacheFailure.STATUS,
        EmailCooldownCacheFailure.DESCRIPTION,
        cause
    )

    class CooldownActive(msg: String, cause: Throwable? = null) : SendEmailAuthenticationException(
        msg,
        EmailCooldownActiveFailure.CODE,
        EmailCooldownActiveFailure.STATUS,
        EmailCooldownActiveFailure.DESCRIPTION,
        cause
    )

    class Template(msg: String, cause: Throwable? = null) : SendEmailAuthenticationException(
        msg,
        EmailTemplateFailure.CODE,
        EmailTemplateFailure.STATUS,
        EmailTemplateFailure.DESCRIPTION,
        cause
    )

    class EmailAuthentication(msg: String, cause: Throwable? = null) : SendEmailAuthenticationException(
        msg,
        EmailAuthenticationFailure.CODE,
        EmailAuthenticationFailure.STATUS,
        EmailAuthenticationFailure.DESCRIPTION,
        cause
    )

    class EmailDisabled(msg: String, cause: Throwable? = null) : SendEmailAuthenticationException(
        msg,
        EmailDisabledFailure.CODE,
        EmailDisabledFailure.STATUS,
        EmailDisabledFailure.DESCRIPTION,
        cause
    )

    class Send(msg: String, cause: Throwable? = null) : SendEmailAuthenticationException(
        msg,
        EmailSendFailure.CODE,
        EmailSendFailure.STATUS,
        EmailSendFailure.DESCRIPTION,
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
