package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.PostCommitSideEffectFailure
import io.stereov.singularity.global.exception.SingularityException
import io.stereov.singularity.principal.core.exception.InvalidUserDocumentFailure
import io.stereov.singularity.principal.core.exception.NoPasswordProvider
import io.stereov.singularity.principal.core.exception.TwoFactorAuthenticationDisabledFailure
import io.stereov.singularity.principal.core.exception.UserNotFoundFailure
import org.springframework.http.HttpStatus

sealed class ValidateTwoFactorException (
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class UserNotFound(msg: String, cause: Throwable? = null) : ValidateTwoFactorException(
        msg,
        UserNotFoundFailure.CODE,
        UserNotFoundFailure.STATUS,
        UserNotFoundFailure.DESCRIPTION,
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : ValidateTwoFactorException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    class NoPasswordSet(msg: String, cause: Throwable? = null) : ValidateTwoFactorException(
        msg,
        NoPasswordProvider.CODE,
        NoPasswordProvider.STATUS,
        NoPasswordProvider.DESCRIPTION,
        cause
    )

    class TwoFactorAuthenticationDisabled(msg: String, cause: Throwable? = null) : ValidateTwoFactorException(
        msg,
        TwoFactorAuthenticationDisabledFailure.CODE,
        TwoFactorAuthenticationDisabledFailure.STATUS,
        TwoFactorAuthenticationDisabledFailure.DESCRIPTION,
        cause
    )

    class InvalidDocument(msg: String, cause: Throwable? = null) : ValidateTwoFactorException(
        msg,
        InvalidUserDocumentFailure.CODE,
        InvalidUserDocumentFailure.STATUS,
        InvalidUserDocumentFailure.DESCRIPTION,
        cause
    )

    class No2faCodeProvided(msg: String, cause: Throwable? = null) : ValidateTwoFactorException(
        msg,
        "NO_2FA_CODE_PROVIDED",
        HttpStatus.BAD_REQUEST,
        "Invalid request: at least one of email or totp must be provided.",
        cause
    )

    class Expired(msg: String, cause: Throwable? = null) : ValidateTwoFactorException(
        msg,
        TwoFactorCodeExpiredFailure.CODE,
        TwoFactorCodeExpiredFailure.STATUS,
        TwoFactorCodeExpiredFailure.DESCRIPTION,
        cause
    )

    class WrongCode(msg: String, cause: Throwable? = null) : ValidateTwoFactorException(
        msg,
        WrongTwoFactorCodeFailure.CODE,
        WrongTwoFactorCodeFailure.STATUS,
        WrongTwoFactorCodeFailure.DESCRIPTION,
        cause
    )

    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : ValidateTwoFactorException(
        msg,
        PostCommitSideEffectFailure.CODE,
        PostCommitSideEffectFailure.STATUS,
        PostCommitSideEffectFailure.DESCRIPTION,
        cause
    )

    class Totp(msg: String, cause: Throwable? = null) : ValidateTwoFactorException(
        msg,
        "TOTP_CODE_VALIDATION_FAILURE",
        HttpStatus.UNAUTHORIZED,
        "Failed to validate TOTP code.",
        cause
    )

    companion object {

        fun fromValidateEmailTwoFactorCode(ex: ValidateEmailTwoFactorCodeException): ValidateTwoFactorException {
            return when (ex) {
                is ValidateEmailTwoFactorCodeException.WrongCode -> WrongCode(ex.message, ex.cause)
                is ValidateEmailTwoFactorCodeException.Expired -> Expired(ex.message, ex.cause)
            }
        }
    }
}
