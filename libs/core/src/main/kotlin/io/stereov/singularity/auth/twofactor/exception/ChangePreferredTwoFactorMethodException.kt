package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.PostCommitSideEffectFailure
import io.stereov.singularity.global.exception.SingularityException
import io.stereov.singularity.principal.core.exception.InvalidUserDocumentFailure
import io.stereov.singularity.principal.core.exception.NoPasswordProvider
import io.stereov.singularity.principal.core.exception.TwoFactorAuthenticationDisabledFailure
import org.springframework.http.HttpStatus

sealed class ChangePreferredTwoFactorMethodException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {


    class Database(msg: String, cause: Throwable? = null) : ChangePreferredTwoFactorMethodException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    class NoPasswordSet(msg: String, cause: Throwable? = null) : ChangePreferredTwoFactorMethodException(
        msg,
        NoPasswordProvider.CODE,
        NoPasswordProvider.STATUS,
        NoPasswordProvider.DESCRIPTION,
        cause
    )

    class TwoFactorDisabled(msg: String, cause: Throwable? = null) : ChangePreferredTwoFactorMethodException(
        msg,
        TwoFactorAuthenticationDisabledFailure.CODE,
        TwoFactorAuthenticationDisabledFailure.STATUS,
        TwoFactorAuthenticationDisabledFailure.DESCRIPTION,
        cause
    )

    class InvalidDocument(msg: String, cause: Throwable? = null) : ChangePreferredTwoFactorMethodException(
        msg,
        InvalidUserDocumentFailure.CODE,
        InvalidUserDocumentFailure.STATUS,
        InvalidUserDocumentFailure.DESCRIPTION,
        cause
    )

    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : ChangePreferredTwoFactorMethodException(
        msg,
        PostCommitSideEffectFailure.CODE,
        PostCommitSideEffectFailure.STATUS,
        PostCommitSideEffectFailure.DESCRIPTION,
        cause
    )
}
