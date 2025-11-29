package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.PostCommitSideEffectFailure
import io.stereov.singularity.database.hash.exception.HashFailure
import io.stereov.singularity.global.exception.SingularityException
import io.stereov.singularity.principal.core.exception.InvalidUserDocumentFailure
import io.stereov.singularity.principal.core.exception.NoPasswordProvider
import org.springframework.http.HttpStatus

sealed class ValidateTotpSetupException (
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class AlreadyEnabled(msg: String, cause: Throwable? = null) : ValidateTotpSetupException(
        msg,
        TwoFactorMethodAlreadyEnabledFailure.CODE,
        TwoFactorMethodAlreadyEnabledFailure.STATUS,
        TwoFactorMethodAlreadyEnabledFailure.DESCRIPTION,
        cause
    )

    class Totp(msg: String, cause: Throwable? = null) : ValidateTotpSetupException(
        msg,
        TotpFailure.CODE,
        TotpFailure.STATUS,
        TotpFailure.DESCRIPTION,
        cause
    )

    class NoPasswordSet(msg: String, cause: Throwable? = null) : ValidateTotpSetupException(
        msg,
        NoPasswordProvider.CODE,
        NoPasswordProvider.STATUS,
        NoPasswordProvider.DESCRIPTION,
        cause
    )

    class InvalidDocument(msg: String, cause: Throwable? = null) : ValidateTotpSetupException(
        msg,
        InvalidUserDocumentFailure.CODE,
        InvalidUserDocumentFailure.STATUS,
        InvalidUserDocumentFailure.DESCRIPTION,
        cause
    )


    class Database(msg: String, cause: Throwable? = null) : ValidateTotpSetupException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    class WrongCode(msg: String, cause: Throwable? = null) : ValidateTotpSetupException(
        msg,
        WrongTwoFactorCodeFailure.CODE,
        WrongTwoFactorCodeFailure.STATUS,
        WrongTwoFactorCodeFailure.DESCRIPTION,
        cause
    )

    class Hash(msg: String, cause: Throwable? = null) : ValidateTotpSetupException(
        msg,
        HashFailure.CODE,
        HashFailure.STATUS,
        HashFailure.DESCRIPTION,
        cause
    )

    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : ValidateTotpSetupException(
        msg,
        PostCommitSideEffectFailure.CODE,
        PostCommitSideEffectFailure.STATUS,
        PostCommitSideEffectFailure.DESCRIPTION,
        cause
    )
}
