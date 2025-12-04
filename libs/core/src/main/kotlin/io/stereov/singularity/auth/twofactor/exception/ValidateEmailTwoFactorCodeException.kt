package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class ValidateEmailTwoFactorCodeException (
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class Expired(msg: String, cause: Throwable? = null) : ValidateEmailTwoFactorCodeException(
        msg,
        TwoFactorCodeExpiredFailure.CODE,
        TwoFactorCodeExpiredFailure.STATUS,
        TwoFactorCodeExpiredFailure.DESCRIPTION,
        cause
    )

    class WrongCode(msg: String, cause: Throwable? = null) : ValidateEmailTwoFactorCodeException(
        msg,
        WrongTwoFactorCodeFailure.CODE,
        WrongTwoFactorCodeFailure.STATUS,
        WrongTwoFactorCodeFailure.DESCRIPTION,
        cause
    )
}
