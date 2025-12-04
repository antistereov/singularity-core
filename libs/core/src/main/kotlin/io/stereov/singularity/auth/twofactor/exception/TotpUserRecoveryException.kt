package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.PostCommitSideEffectFailure
import io.stereov.singularity.database.hash.exception.HashFailure
import io.stereov.singularity.global.exception.SingularityException
import io.stereov.singularity.principal.core.exception.InvalidUserDocumentFailure
import io.stereov.singularity.principal.core.exception.NoPasswordProvider
import io.stereov.singularity.principal.core.exception.UserNotFoundFailure
import org.springframework.http.HttpStatus

sealed class TotpUserRecoveryException (
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class UserNotFound(msg: String, cause: Throwable? = null) : TotpUserRecoveryException(
        msg,
        UserNotFoundFailure.CODE,
        UserNotFoundFailure.STATUS,
        UserNotFoundFailure.DESCRIPTION,
        cause
    )

    class NoPasswordSet(msg: String, cause: Throwable? = null) : TotpUserRecoveryException(
        msg,
        NoPasswordProvider.CODE,
        NoPasswordProvider.STATUS,
        NoPasswordProvider.DESCRIPTION,
        cause
    )

    class InvalidDocument(msg: String, cause: Throwable? = null) : TotpUserRecoveryException(
        msg,
        InvalidUserDocumentFailure.CODE,
        InvalidUserDocumentFailure.STATUS,
        InvalidUserDocumentFailure.DESCRIPTION,
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : TotpUserRecoveryException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    class WrongCode(msg: String, cause: Throwable? = null) : TotpUserRecoveryException(
        msg,
        WrongTwoFactorCodeFailure.CODE,
        WrongTwoFactorCodeFailure.STATUS,
        WrongTwoFactorCodeFailure.DESCRIPTION,
        cause
    )

    class Hash(msg: String, cause: Throwable? = null) : TotpUserRecoveryException(
        msg,
        HashFailure.CODE,
        HashFailure.STATUS,
        HashFailure.DESCRIPTION,
        cause
    )

    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : TotpUserRecoveryException(
        msg,
        PostCommitSideEffectFailure.CODE,
        PostCommitSideEffectFailure.STATUS,
        PostCommitSideEffectFailure.DESCRIPTION,
        cause
    )

    class MethodDisabled(msg: String, cause: Throwable? = null) : TotpUserRecoveryException(
        msg,
        TwoFactorMethodDisabledFailure.CODE,
        TwoFactorMethodDisabledFailure.STATUS,
        TwoFactorMethodDisabledFailure.DESCRIPTION,
        cause
    )
}
