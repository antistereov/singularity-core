package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.PostCommitSideEffectFailure
import io.stereov.singularity.database.hash.exception.HashFailure
import io.stereov.singularity.global.exception.SingularityException
import io.stereov.singularity.principal.core.exception.InvalidUserDocumentFailure
import io.stereov.singularity.principal.core.exception.NoPasswordProvider
import io.stereov.singularity.principal.core.exception.UserNotFoundFailure
import org.springframework.http.HttpStatus

sealed class DisableTotpException (
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class AlreadyDisabled(msg: String, cause: Throwable? = null) : DisableTotpException(
        msg,
        TwoFactorMethodAlreadyDisabledFailure.CODE,
        TwoFactorMethodAlreadyDisabledFailure.STATUS,
        TwoFactorMethodAlreadyDisabledFailure.DESCRIPTION,
        cause
    )

    class CannotDisableOnlyTwoFactorMethod(msg: String, cause: Throwable? = null) : DisableTotpException(
        msg,
        CannotDisableOnlyTwoFactorMethodFailure.CODE,
        CannotDisableOnlyTwoFactorMethodFailure.STATUS,
        CannotDisableOnlyTwoFactorMethodFailure.DESCRIPTION,
        cause
    )

    class UserNotFound(msg: String, cause: Throwable? = null) : DisableTotpException(
        msg,
        UserNotFoundFailure.CODE,
        UserNotFoundFailure.STATUS,
        UserNotFoundFailure.DESCRIPTION,
        cause
    )

    class NoPasswordSet(msg: String, cause: Throwable? = null) : DisableTotpException(
        msg,
        NoPasswordProvider.CODE,
        NoPasswordProvider.STATUS,
        NoPasswordProvider.DESCRIPTION,
        cause
    )

    class InvalidDocument(msg: String, cause: Throwable? = null) : DisableTotpException(
        msg,
        InvalidUserDocumentFailure.CODE,
        InvalidUserDocumentFailure.STATUS,
        InvalidUserDocumentFailure.DESCRIPTION,
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : DisableTotpException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    class Hash(msg: String, cause: Throwable? = null) : DisableTotpException(
        msg,
        HashFailure.CODE,
        HashFailure.STATUS,
        HashFailure.DESCRIPTION,
        cause
    )

    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : DisableTotpException(
        msg,
        PostCommitSideEffectFailure.CODE,
        PostCommitSideEffectFailure.STATUS,
        PostCommitSideEffectFailure.DESCRIPTION,
        cause
    )
}
