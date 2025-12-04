package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.PostCommitSideEffectFailure
import io.stereov.singularity.global.exception.SingularityException
import io.stereov.singularity.principal.core.exception.InvalidUserDocumentFailure
import io.stereov.singularity.principal.core.exception.NoPasswordProvider
import org.springframework.http.HttpStatus

sealed class DisableEmailAuthenticationException (
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class AlreadyDisabled(msg: String, cause: Throwable? = null) : DisableEmailAuthenticationException(
        msg,
        TwoFactorMethodAlreadyDisabledFailure.CODE,
        TwoFactorMethodAlreadyDisabledFailure.STATUS,
        TwoFactorMethodAlreadyDisabledFailure.DESCRIPTION,
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : DisableEmailAuthenticationException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    class NoPasswordSet(msg: String, cause: Throwable? = null) : DisableEmailAuthenticationException(
        msg,
        NoPasswordProvider.CODE,
        NoPasswordProvider.STATUS,
        NoPasswordProvider.DESCRIPTION,
        cause
    )

    class InvalidDocument(msg: String, cause: Throwable? = null) : DisableEmailAuthenticationException(
        msg,
        InvalidUserDocumentFailure.CODE,
        InvalidUserDocumentFailure.STATUS,
        InvalidUserDocumentFailure.DESCRIPTION,
        cause
    )

    class Expired(msg: String, cause: Throwable? = null) : DisableEmailAuthenticationException(
        msg,
        TwoFactorCodeExpiredFailure.CODE,
        TwoFactorCodeExpiredFailure.STATUS,
        TwoFactorCodeExpiredFailure.DESCRIPTION,
        cause
    )

    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : DisableEmailAuthenticationException(
        msg,
        PostCommitSideEffectFailure.CODE,
        PostCommitSideEffectFailure.STATUS,
        PostCommitSideEffectFailure.DESCRIPTION,
        cause
    )

    class CannotDisableOnlyTwoFactorMethod(msg: String, cause: Throwable? = null) : DisableEmailAuthenticationException(
        msg,
        CannotDisableOnlyTwoFactorMethodFailure.CODE,
        CannotDisableOnlyTwoFactorMethodFailure.STATUS,
        CannotDisableOnlyTwoFactorMethodFailure.DESCRIPTION,
        cause
    )

}
