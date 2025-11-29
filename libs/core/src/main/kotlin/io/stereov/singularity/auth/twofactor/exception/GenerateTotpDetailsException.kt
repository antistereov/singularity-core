package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.global.exception.SingularityException
import io.stereov.singularity.principal.core.exception.InvalidUserDocumentFailure
import io.stereov.singularity.principal.core.exception.NoPasswordProvider
import org.springframework.http.HttpStatus

sealed class GenerateTotpDetailsException (
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class AlreadyEnabled(msg: String, cause: Throwable? = null) : GenerateTotpDetailsException(
        msg,
        TwoFactorMethodAlreadyEnabledFailure.CODE,
        TwoFactorMethodAlreadyEnabledFailure.STATUS,
        TwoFactorMethodAlreadyEnabledFailure.DESCRIPTION,
        cause
    )

    class Totp(msg: String, cause: Throwable? = null) : GenerateTotpDetailsException(
        msg,
        TotpFailure.CODE,
        TotpFailure.STATUS,
        TotpFailure.DESCRIPTION,
        cause
    )

    class NoPasswordSet(msg: String, cause: Throwable? = null) : GenerateTotpDetailsException(
        msg,
        NoPasswordProvider.CODE,
        NoPasswordProvider.STATUS,
        NoPasswordProvider.DESCRIPTION,
        cause
    )

    class InvalidDocument(msg: String, cause: Throwable? = null) : GenerateTotpDetailsException(
        msg,
        InvalidUserDocumentFailure.CODE,
        InvalidUserDocumentFailure.STATUS,
        InvalidUserDocumentFailure.DESCRIPTION,
        cause
    )

    class TokenCreation(msg: String, cause: Throwable? = null) : GenerateTotpDetailsException(
        msg,
        "TOTP_TOKEN_CREATION_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to create TOTP token.",
        cause
    )

    class InvalidConfiguration(msg: String, cause: Throwable? = null) : GenerateTotpDetailsException(
        msg,
        "INVALID_CONFIGURATION",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Invalid configuration.",
        cause
    )
}
