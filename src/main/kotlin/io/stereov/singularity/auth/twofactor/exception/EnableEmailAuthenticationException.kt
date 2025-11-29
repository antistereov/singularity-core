package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.PostCommitSideEffectFailure
import io.stereov.singularity.global.exception.SingularityException
import io.stereov.singularity.principal.core.exception.InvalidUserDocumentFailure
import io.stereov.singularity.principal.core.exception.NoPasswordProvider
import org.springframework.http.HttpStatus

sealed class EnableEmailAuthenticationException (
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class AlreadyEnabled(msg: String, cause: Throwable? = null) : EnableEmailAuthenticationException(
        msg,
        TwoFactorMethodAlreadyEnabledFailure.CODE,
        TwoFactorMethodAlreadyEnabledFailure.STATUS,
        TwoFactorMethodAlreadyEnabledFailure.DESCRIPTION,
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : EnableEmailAuthenticationException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    class NoPasswordSet(msg: String, cause: Throwable? = null) : EnableEmailAuthenticationException(
        msg,
        NoPasswordProvider.CODE,
        NoPasswordProvider.STATUS,
        NoPasswordProvider.DESCRIPTION,
        cause
    )

    class InvalidDocument(msg: String, cause: Throwable? = null) : EnableEmailAuthenticationException(
        msg,
        InvalidUserDocumentFailure.CODE,
        InvalidUserDocumentFailure.STATUS,
        InvalidUserDocumentFailure.DESCRIPTION,
        cause
    )

    class Expired(msg: String, cause: Throwable? = null) : EnableEmailAuthenticationException(
        msg,
        TwoFactorCodeExpiredFailure.CODE,
        TwoFactorCodeExpiredFailure.STATUS,
        TwoFactorCodeExpiredFailure.DESCRIPTION,
        cause
    )

    class WrongCode(msg: String, cause: Throwable? = null) : EnableEmailAuthenticationException(
        msg,
        WrongTwoFactorCodeFailure.CODE,
        WrongTwoFactorCodeFailure.STATUS,
        WrongTwoFactorCodeFailure.DESCRIPTION,
        cause
    )

    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : EnableEmailAuthenticationException(
        msg,
        PostCommitSideEffectFailure.CODE,
        PostCommitSideEffectFailure.STATUS,
        PostCommitSideEffectFailure.DESCRIPTION,
        cause
    )

    companion object {

        fun fromValidateEmailTwoFactorCode(ex: ValidateEmailTwoFactorCodeException): EnableEmailAuthenticationException {
            return when (ex) {
                is ValidateEmailTwoFactorCodeException.WrongCode -> WrongCode(ex.message, ex.cause)
                is ValidateEmailTwoFactorCodeException.Expired -> Expired(ex.message, ex.cause)
            }
        }
    }

}
